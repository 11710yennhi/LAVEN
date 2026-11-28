package iuh.fit.laven.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import iuh.fit.laven.dto.request.order.ReceiveReturnRequest;
import iuh.fit.laven.dto.request.order.ReturnRequest;
import iuh.fit.laven.dto.request.order.StaffReturnFilterRequest;
import iuh.fit.laven.dto.response.order.ReturnDetailResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.OrderDetail;
import iuh.fit.laven.model.ReturnEvidence;
import iuh.fit.laven.model.ReturnItem;
import iuh.fit.laven.model.id.ReturnItemId;
import iuh.fit.laven.model.ReturnRecord;
import iuh.fit.laven.model.ShipCost;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.OrderDetailRepository;
import iuh.fit.laven.repository.OrderRepository;
import iuh.fit.laven.repository.ReturnEvidenceRepository;
import iuh.fit.laven.repository.ReturnItemRepository;
import iuh.fit.laven.repository.ReturnRecordRepository;
import iuh.fit.laven.repository.ShipCostRepository;
import iuh.fit.laven.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnEvidenceRepository returnEvidenceRepository;
    private final ShipCostRepository shipCostRepository;
    private final ColorStockRepository colorStockRepository;
    private final UserRepository userRepository;

    private final CloudinaryUploadService cloudinaryUploadService;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024;
    private static final int MAX_NOTE_LENGTH = 500;
    private static final int RETURN_REQUEST_DAYS = 7;
    /** Sau khi APPROVED, khách có 7 ngày để gửi hàng (quy trình 12 bước 4). */
    private static final int SEND_BACK_DAYS = 7;
    private static final String MANUFACTURER_DEFECT_NOTE = "Lỗi do nhà sản xuất (tự ghi khi duyệt)";
    private static final String AUTO_REJECT_REASON = "Quá 7 ngày không gửi hàng";

    private static final DateTimeFormatter EVIDENCE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS");


    @Transactional
    public void requestReturn(String orderId, String customerId, ReturnRequest request) {
        // 1. Kiểm tra đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền yêu cầu trả hàng");
        }
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được trả hàng với đơn đã hoàn thành");
        }
        if (order.getCompletedAt() == null
                || LocalDateTime.now().isAfter(order.getCompletedAt().plusDays(RETURN_REQUEST_DAYS))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đã quá thời hạn yêu cầu trả hàng");
        }
        if (returnRecordRepository.existsById(orderId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng đã có yêu cầu trả hàng");
        }

        List<OrderDetail> orderDetails = orderDetailRepository.findAllById_OrderId(orderId);
        if (orderDetails.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Đơn hàng không có sản phẩm");
        }

        // 2. Kiểm tra dữ liệu khách gửi (trước khi upload để không tốn dung lượng Cloudinary)
        if (request.getReturnReason() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do trả hàng không hợp lệ");
        }
        ReturnReason reason = request.getReturnReason();

        validateRefundBankInformation(order, request);
        validateReturnInformation(request);
        validateFiles(request.getVideos(), request.getImages());

        // 3. Tạo danh sách món trả, tính phí ship chiều về và tiền hoàn (phần g)
        List<ReturnItem> returnItems = buildReturnItems(order, orderDetails, request);
        int returnShippingCost = calculateReturnShippingCost(order, reason);
        int refundAmount = calculateRefundAmount(order, orderDetails, returnItems, reason);

        // 4. Upload bằng chứng và lưu. Lỗi ở bất kỳ bước nào thì xóa file đã upload.
        List<Map> uploadedFiles = new ArrayList<>();
        try {
            List<ReturnEvidence> evidences = uploadEvidenceFiles(order, request, uploadedFiles);

            ReturnRecord returnRecord = ReturnRecord.builder()
                    .order(order)
                    .returnType(ReturnType.CUSTOMER_RETURN)
                    .returnReason(reason)
                    .note(request.getNote())
                    .status(ReturnStatus.REQUESTED)
                    .refundBankName(request.getRefundBankName())
                    .refundAccountNumber(request.getRefundAccountNumber())
                    .refundAccountName(request.getRefundAccountName())
                    .createdAt(LocalDateTime.now())
                    .returnShippingCost(returnShippingCost)
                    .refundAmount(refundAmount)
                    .build();

            returnRecordRepository.save(returnRecord);
            returnItemRepository.saveAll(returnItems);
            returnEvidenceRepository.saveAll(evidences);

            // Ép ghi xuống DB ngay để lỗi DB (trùng khóa, quá độ dài cột...)
            // xảy ra trong try, nhờ đó file trên Cloudinary được dọn.
            returnRecordRepository.flush();
        } catch (RuntimeException e) {
            deleteUploadedFiles(uploadedFiles);
            throw e;
        }
    }

    // =====================================================================
    // Khách xem yêu cầu trả hàng của đơn mình
    // =====================================================================
    @Transactional(readOnly = true)
    public ReturnDetailResponse getCustomerReturn(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!order.getCustomer().getUserId().equals(customerId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem yêu cầu trả hàng này");
        }
        // Phiếu FAILED_DELIVERY là nghiệp vụ nội bộ của shop, không phải yêu cầu của khách
        ReturnRecord record = returnRecordRepository.findById(orderId)
                .filter(r -> r.getReturnType() == ReturnType.CUSTOMER_RETURN)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Đơn hàng chưa có yêu cầu trả hàng"));
        return toResponse(record);
    }

    // =====================================================================
    // Staff xem danh sách và chi tiết phiếu trả hàng / giao thất bại
    // =====================================================================
    @Transactional(readOnly = true)
    public Page<ReturnDetailResponse> getStaffReturns(StaffReturnFilterRequest filter) {
        validateStaffReturnFilter(filter);
        Specification<ReturnRecord> spec = (root, query, cb) -> cb.conjunction();
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        if (filter.getReturnType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("returnType"), filter.getReturnType()));
        }
        if (filter.getReturnReason() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("returnReason"), filter.getReturnReason()));
        }
        if (filter.getOrderId() != null && !filter.getOrderId().isBlank()) {
            String keyword = filter.getOrderId().trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("orderId")), "%" + keyword + "%"));
        }
        if (filter.getCustomerName() != null && !filter.getCustomerName().isBlank()) {
            String keyword = "%" + filter.getCustomerName().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("order").get("customer").get("fullName")), keyword),
                    cb.like(cb.lower(root.get("order").get("receiverName")), keyword)));
        }
        if (filter.getCreatedFrom() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
        }
        if (filter.getCreatedTo() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
        }
        if (filter.getMinRefundAmount() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("refundAmount"), filter.getMinRefundAmount()));
        }
        if (filter.getMaxRefundAmount() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("refundAmount"), filter.getMaxRefundAmount()));
        }
        int page = filter.getPage() == null ? 0 : filter.getPage();
        int size = filter.getSize() == null ? 10 : filter.getSize();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReturnRecord> records = returnRecordRepository.findAll(spec, pageable);
        return new PageImpl<>(toResponses(records.getContent()), pageable, records.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReturnDetailResponse getStaffReturn(String orderId) {
        ReturnRecord record = returnRecordRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu trả hàng"));
        return toResponse(record);
    }

    // =====================================================================
    // UC26 - Staff duyệt: REQUESTED → APPROVED (quy trình 12 bước 3, bảng tồn kho quy trình 16)
    // =====================================================================
    @Transactional
    public ReturnDetailResponse approveReturn(String orderId) {
        ReturnRecord record = findCustomerReturnForUpdate(orderId);
        requireStatus(record, ReturnStatus.REQUESTED, "Chỉ duyệt được yêu cầu đang chờ duyệt");

        Order order = record.getOrder();
        LocalDateTime now = LocalDateTime.now();
        List<ReturnItem> items = returnItemRepository.findAllById_OrderId(orderId);

        record.setStatus(ReturnStatus.APPROVED);
        record.setApprovedAt(now);

        switch (record.getReturnReason()) {
            case MANUFACTURER_DEFECT -> {
                // Hàng lỗi NSX: toàn bộ coi là hư hỏng, khi nhận về không cộng kho
                for (ReturnItem item : items) {
                    item.setDamagedQuantity(item.getQuantity());
                    if (isBlank(item.getInspectionNote())) {
                        item.setInspectionNote(MANUFACTURER_DEFECT_NOTE);
                    }
                }
            }
            case WRONG_ITEM -> {
                // Món A (món đặt) đã bị trừ lúc đặt nhưng thực tế vẫn còn trong kho → cộng lại
                for (ReturnItem item : items) {
                    ColorStock stockA = lockStock(item.getVariant().getVariantId(), item.getColor().getColorId());
                    changeStock(stockA, item.getQuantity());
                }
            }
            case NOT_RECEIVED -> {
                // Hàng thất lạc: không đổi kho, phiếu kết thúc tại APPROVED
                order.setOrderStatus(OrderStatus.RETURNED);
                markRefundPending(order);
                order.setFinalizedAt(now);
            }
            default -> {
                // NOT_AS_DESCRIBED, OTHER: chờ hàng về mới cộng kho
            }
        }

        returnItemRepository.saveAll(items);
        returnRecordRepository.save(record);
        orderRepository.save(order);
        return toResponse(record);
    }

    // =====================================================================
    // UC26 - Staff từ chối: REQUESTED → REJECTED, chốt sổ đơn
    // =====================================================================
    @Transactional
    public ReturnDetailResponse rejectReturn(String orderId, String rejectionReason) {
        if (isBlank(rejectionReason)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do từ chối không được rỗng");
        }
        String cleanReason = rejectionReason.trim();
        if (cleanReason.length() > MAX_NOTE_LENGTH) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do từ chối tối đa 500 ký tự");
        }

        ReturnRecord record = findCustomerReturnForUpdate(orderId);
        requireStatus(record, ReturnStatus.REQUESTED, "Chỉ từ chối được yêu cầu đang chờ duyệt");

        record.setStatus(ReturnStatus.REJECTED);
        record.setRejectionReason(cleanReason);

        // Đơn vẫn COMPLETED; phiếu kết thúc nên chốt sổ doanh thu
        Order order = record.getOrder();
        order.setFinalizedAt(LocalDateTime.now());

        returnRecordRepository.save(record);
        orderRepository.save(order);
        return toResponse(record);
    }

    // =====================================================================
    // UC27 - Khách đã gửi hàng cho đơn vị vận chuyển: APPROVED → IN_TRANSIT (giả lập)
    // =====================================================================
    @Transactional
    public ReturnDetailResponse markReturnInTransit(String orderId) {
        ReturnRecord record = findCustomerReturnForUpdate(orderId);
        requireStatus(record, ReturnStatus.APPROVED, "Chỉ cập nhật được yêu cầu đã được duyệt");

        if (record.getReturnReason() == ReturnReason.NOT_RECEIVED) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không nhận được hàng đã kết thúc khi duyệt, không có hàng gửi về");
        }
        if (record.getApprovedAt() != null
                && LocalDateTime.now().isAfter(record.getApprovedAt().plusDays(SEND_BACK_DAYS))) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Đã quá 7 ngày kể từ khi duyệt, khách không gửi hàng đúng hạn");
        }

        // Từ đây không tính hạn 7 ngày nữa
        record.setStatus(ReturnStatus.IN_TRANSIT);
        returnRecordRepository.save(record);
        return toResponse(record);
    }

    // =====================================================================
    // UC28 - Staff nhận và kiểm tra hàng: IN_TRANSIT → RECEIVED
    // Dùng cho cả phiếu giao thất bại (quy trình 11 bước 5) và trả hàng (quy trình 12 bước 6).
    // =====================================================================
    @Transactional
    public ReturnDetailResponse receiveReturn(String orderId, String staffId, ReceiveReturnRequest request) {
        ReturnRecord record = findReturnForUpdate(orderId);
        requireStatus(record, ReturnStatus.IN_TRANSIT, "Chỉ nhận được hàng đang trên đường về");

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));

        List<ReturnItem> items = returnItemRepository.findAllById_OrderId(orderId);
        Map<String, ReceiveReturnRequest.Item> inputs = indexReceiveInputs(request, items);

        boolean failedDelivery = record.getReturnType() == ReturnType.FAILED_DELIVERY;
        ReturnReason reason = record.getReturnReason();

        for (ReturnItem item : items) {
            ReceiveReturnRequest.Item input =
                    inputs.get(itemKey(item.getVariant().getVariantId(), item.getColor().getColorId()));
            String inspectionNote = input == null || isBlank(input.getInspectionNote())
                    ? null : input.getInspectionNote().trim();
            if (inspectionNote != null && inspectionNote.length() > MAX_NOTE_LENGTH) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Ghi chú kiểm tra tối đa 500 ký tự");
            }

            // MANUFACTURER_DEFECT: damagedQuantity = quantity từ lúc duyệt, RECEIVED không đổi kho
            if (!failedDelivery && reason == ReturnReason.MANUFACTURER_DEFECT) {
                if (inspectionNote != null) {
                    item.setInspectionNote(inspectionNote);
                }
                continue;
            }

            int damaged = input == null || input.getDamagedQuantity() == null ? 0 : input.getDamagedQuantity();
            if (damaged < 0 || damaged > item.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng hư hỏng không hợp lệ");
            }
            if (damaged > 0 && inspectionNote == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Ghi chú kiểm tra không được rỗng khi có hàng hư hỏng");
            }
            item.setDamagedQuantity(damaged);
            item.setInspectionNote(inspectionNote);

            if (!failedDelivery && reason == ReturnReason.WRONG_ITEM) {
                // Món B (shop gửi nhầm) chưa từng bị trừ kho: chỉ trừ phần B hư hỏng
                if (input == null || isBlank(input.getReceivedVariantId())) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm thực nhận không được rỗng");
                }
                if (isBlank(input.getReceivedColorId())) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Màu sản phẩm thực nhận không được rỗng");
                }
                ColorStock stockB = lockStock(input.getReceivedVariantId().trim(),
                        normalizeColorId(input.getReceivedColorId()));
                item.setReceivedVariant(stockB.getVariant());
                item.setReceivedColor(stockB.getColor());
                changeStock(stockB, -damaged);
            } else {
                // FAILED_DELIVERY, NOT_AS_DESCRIBED, OTHER: cộng phần còn bán được vào món trả
                ColorStock stockA = lockStock(item.getVariant().getVariantId(), item.getColor().getColorId());
                changeStock(stockA, item.getQuantity() - damaged);
            }
        }

        record.setStatus(ReturnStatus.RECEIVED);
        record.setReceivedAt(LocalDateTime.now());
        record.setReceivedBy(staff);

        Order order = record.getOrder();
        if (!failedDelivery) {
            // Trả hàng: đơn → RETURNED, chờ hoàn tiền, chốt sổ.
            // Giao thất bại: đơn giữ DELIVERY_FAILED (đã REFUND_PENDING lúc giao thất bại nếu đã PAID),
            // không chốt sổ vì đơn giao thất bại không có doanh thu.
            order.setOrderStatus(OrderStatus.RETURNED);
            markRefundPending(order);
            order.setFinalizedAt(LocalDateTime.now());
        }

        returnItemRepository.saveAll(items);
        returnRecordRepository.save(record);
        orderRepository.save(order);
        return toResponse(record);
    }

    // =====================================================================
    // UC36 - Hệ thống: APPROVED quá 7 ngày chưa gửi hàng → REJECTED (quy trình 12 bước 4)
    // Gọi từ scheduler/ReturnDeadlineJob mỗi ngày.
    // =====================================================================
    @Transactional
    public int rejectExpiredApprovedReturns() {
        LocalDateTime now = LocalDateTime.now();
        List<String> orderIds = returnRecordRepository.findExpiredApprovedOrderIds(
                ReturnType.CUSTOMER_RETURN, ReturnStatus.APPROVED,
                ReturnReason.NOT_RECEIVED, now.minusDays(SEND_BACK_DAYS));

        int rejected = 0;
        for (String orderId : orderIds) {
            // Khóa đơn rồi kiểm tra lại: Staff có thể vừa bấm in-transit trước khi job tới phiếu này
            ReturnRecord record = findReturnForUpdate(orderId);
            if (record.getStatus() != ReturnStatus.APPROVED) {
                continue;
            }
            record.setStatus(ReturnStatus.REJECTED);
            record.setRejectionReason(AUTO_REJECT_REASON);

            // Đơn giữ COMPLETED, không được yêu cầu trả nữa; phiếu kết thúc nên chốt sổ
            Order order = record.getOrder();
            order.setFinalizedAt(now);

            returnRecordRepository.save(record);
            orderRepository.save(order);
            rejected++;
        }
        return rejected;
    }

    /** Khóa dòng Order để 2 nhân viên không xử lý cùng một phiếu cùng lúc. */
    private ReturnRecord findReturnForUpdate(String orderId) {
        orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        return returnRecordRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Đơn hàng không có phiếu trả hàng"));
    }

    private ReturnRecord findCustomerReturnForUpdate(String orderId) {
        ReturnRecord record = findReturnForUpdate(orderId);
        if (record.getReturnType() != ReturnType.CUSTOMER_RETURN) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Phiếu giao hàng thất bại không áp dụng thao tác này");
        }
        return record;
    }

    private void requireStatus(ReturnRecord record, ReturnStatus expected, String message) {
        if (record.getStatus() != expected) {
            throw new AppException(HttpStatus.BAD_REQUEST, message + " (trạng thái hiện tại: " + record.getStatus() + ")");
        }
    }

    /** Bảng chuyển trạng thái PaymentStatus: PAID → REFUND_PENDING khi đơn RETURNED. */
    private void markRefundPending(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
    }

    /** Khóa bi quan dòng ColorStock trước khi đổi tồn kho (thiết kế i.7). */
    private ColorStock lockStock(String variantId, String colorId) {
        return colorStockRepository.findByVariantAndColorForUpdate(variantId, colorId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không có màu này"));
    }

    private void changeStock(ColorStock stock, int delta) {
        long updated = (long) stock.getStock() + delta;
        if (updated < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tồn kho không đủ để trừ số lượng hư hỏng");
        }
        stock.setStock(Math.toIntExact(updated));
    }

    /** Kiểm tra dữ liệu nhận hàng: không trùng món, món phải thuộc phiếu trả. */
    private Map<String, ReceiveReturnRequest.Item> indexReceiveInputs(
            ReceiveReturnRequest request, List<ReturnItem> items) {
        Map<String, ReceiveReturnRequest.Item> result = new HashMap<>();
        if (request == null || request.getItems() == null) {
            return result;
        }
        Set<String> returnItemKeys = new HashSet<>();
        for (ReturnItem item : items) {
            returnItemKeys.add(itemKey(item.getVariant().getVariantId(), item.getColor().getColorId()));
        }
        for (ReceiveReturnRequest.Item input : request.getItems()) {
            if (input == null || isBlank(input.getVariantId()) || isBlank(input.getColorId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu mã biến thể hoặc mã màu của món trả");
            }
            String key = itemKey(input.getVariantId().trim(), normalizeColorId(input.getColorId()));
            if (!returnItemKeys.contains(key)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không thuộc phiếu trả hàng");
            }
            if (result.put(key, input) != null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Không được nhập trùng sản phẩm và màu sắc");
            }
        }
        return result;
    }

    private void validateStaffReturnFilter(StaffReturnFilterRequest filter) {
        if (filter.getCreatedFrom() != null && filter.getCreatedTo() != null
                && filter.getCreatedFrom().isAfter(filter.getCreatedTo())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
        if ((filter.getMinRefundAmount() != null && filter.getMinRefundAmount() < 0)
                || (filter.getMaxRefundAmount() != null && filter.getMaxRefundAmount() < 0)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số tiền hoàn không hợp lệ");
        }
        if (filter.getMinRefundAmount() != null && filter.getMaxRefundAmount() != null
                && filter.getMinRefundAmount() > filter.getMaxRefundAmount()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số tiền hoàn tối thiểu không được lớn hơn tối đa");
        }
        if (filter.getPage() != null && filter.getPage() < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số trang không hợp lệ");
        }
        if (filter.getSize() != null && (filter.getSize() <= 0 || filter.getSize() > 100)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Kích thước trang phải từ 1 đến 100");
        }
    }

    private String itemKey(String variantId, String colorId) {
        return variantId + "_" + normalizeColorId(colorId);
    }

    /** Mã màu lưu chữ hoa trong DB (#093F69). */
    private String normalizeColorId(String colorId) {
        return colorId == null ? null : colorId.trim().toUpperCase();
    }

    private ReturnDetailResponse toResponse(ReturnRecord record) {
        return toResponses(List.of(record)).get(0);
    }

    /** Map nhiều phiếu: chi tiết đơn, món trả, bằng chứng của cả nhóm tải bằng 3 truy vấn IN (tránh N+1). */
    private List<ReturnDetailResponse> toResponses(List<ReturnRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> orderIds = records.stream().map(ReturnRecord::getOrderId).toList();
        Map<String, List<OrderDetail>> detailsByOrder = orderDetailRepository.findAllById_OrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(d -> d.getId().getOrderId()));
        Map<String, List<ReturnItem>> itemsByOrder = returnItemRepository.findAllWithProductByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(i -> i.getId().getOrderId()));
        Map<String, List<String>> evidenceByOrder = returnEvidenceRepository
                .findAllByOrder_OrderIdInOrderByReturnEvidenceIdAsc(orderIds).stream()
                .collect(Collectors.groupingBy(e -> e.getOrder().getOrderId(),
                        Collectors.mapping(ReturnEvidence::getMediaUrl, Collectors.toList())));

        List<ReturnDetailResponse> result = new ArrayList<>();
        for (ReturnRecord record : records) {
            String orderId = record.getOrderId();
            result.add(toResponse(record,
                    detailsByOrder.getOrDefault(orderId, List.of()),
                    itemsByOrder.getOrDefault(orderId, List.of()),
                    evidenceByOrder.getOrDefault(orderId, List.of())));
        }
        return result;
    }

    private ReturnDetailResponse toResponse(ReturnRecord record, List<OrderDetail> orderDetails,
                                            List<ReturnItem> returnItems, List<String> evidenceUrls) {
        Order order = record.getOrder();
        String orderId = order.getOrderId();

        List<ReturnDetailResponse.Item> items = new ArrayList<>();
        for (ReturnItem item : returnItems) {
            Integer unitPrice = orderDetails.stream()
                    .filter(d -> d.getVariant().getVariantId().equals(item.getVariant().getVariantId())
                            && d.getColor().getColorId().equals(item.getColor().getColorId()))
                    .findFirst()
                    .map(OrderDetail::getUnitPrice)
                    .orElse(null);
            items.add(ReturnDetailResponse.Item.builder()
                    .productId(item.getVariant().getProduct().getProductId())
                    .productDisplayName(item.getVariant().getProduct().getProductDisplayName())
                    .variantId(item.getVariant().getVariantId())
                    .size(item.getVariant().getSize())
                    .colorId(item.getColor().getColorId())
                    .colorName(item.getColor().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .damagedQuantity(item.getDamagedQuantity())
                    .inspectionNote(item.getInspectionNote())
                    .receivedVariantId(item.getReceivedVariant() == null ? null : item.getReceivedVariant().getVariantId())
                    .receivedColorId(item.getReceivedColor() == null ? null : item.getReceivedColor().getColorId())
                    .build());
        }

        LocalDateTime sendBackDeadline = record.getStatus() == ReturnStatus.APPROVED
                && record.getReturnReason() != ReturnReason.NOT_RECEIVED
                && record.getApprovedAt() != null
                ? record.getApprovedAt().plusDays(SEND_BACK_DAYS)
                : null;

        return ReturnDetailResponse.builder()
                .orderId(orderId)
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .receiverName(order.getReceiverName())
                .returnType(record.getReturnType())
                .returnReason(record.getReturnReason())
                .status(record.getStatus())
                .note(record.getNote())
                .rejectionReason(record.getRejectionReason())
                .returnShippingCost(record.getReturnShippingCost())
                .refundAmount(record.getRefundAmount())
                .refundBankName(record.getRefundBankName())
                .refundAccountNumber(record.getRefundAccountNumber())
                .refundAccountName(record.getRefundAccountName())
                .createdAt(record.getCreatedAt())
                .approvedAt(record.getApprovedAt())
                .sendBackDeadline(sendBackDeadline)
                .receivedAt(record.getReceivedAt())
                .receivedByName(record.getReceivedBy() == null ? null : record.getReceivedBy().getFullName())
                .items(items)
                .evidenceUrls(evidenceUrls)
                .build();
    }

    private void validateRefundBankInformation(Order order, ReturnRequest request) {
        if (order.getPaymentMethod() == PaymentMethod.COD
                && (isBlank(request.getRefundBankName())
                    || isBlank(request.getRefundAccountNumber())
                    || isBlank(request.getRefundAccountName()))) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin tài khoản nhận hoàn tiền không được rỗng"
            );
        }
    }

    private void validateReturnInformation(ReturnRequest request) {
        if (request.getNote() != null && request.getNote().length() > MAX_NOTE_LENGTH) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ghi chú trả hàng tối đa 500 ký tự");
        }

        if (request.getReturnReason() == ReturnReason.NOT_RECEIVED) {
            return;
        }

        if (isBlank(request.getNote())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ghi chú trả hàng không được rỗng");
        }

        if (request.getVideos() == null || request.getVideos().stream().noneMatch(this::hasFile)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng tải lên ít nhất một video kiểm hàng");
        }
    }


    private List<ReturnItem> buildReturnItems(
            Order order,
            List<OrderDetail> orderDetails,
            ReturnRequest request
    ) {
        List<ReturnItem> result = new ArrayList<>();

        if (request.getReturnReason() == ReturnReason.NOT_RECEIVED) {
            for (OrderDetail detail : orderDetails) {
                result.add(newReturnItem(order, detail, detail.getQuantity()));
            }
            return result;
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất một sản phẩm cần trả");
        }

        Set<String> selectedItems = new HashSet<>();
        for (ReturnRequest.ReturnItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0");
            }

            String itemKey = itemRequest.getVariantId() + "_" + itemRequest.getColorId().trim().toUpperCase();
            if (!selectedItems.add(itemKey)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Không được chọn trùng sản phẩm và màu sắc");
            }

            OrderDetail purchasedDetail = findOrderDetail(
                    orderDetails, itemRequest.getVariantId(), itemRequest.getColorId());

            if (itemRequest.getQuantity() > purchasedDetail.getQuantity()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Số lượng trả vượt quá số lượng đã mua");
            }

            result.add(newReturnItem(order, purchasedDetail, itemRequest.getQuantity()));
        }

        return result;
    }


    private ReturnItem newReturnItem(Order order, OrderDetail detail, int quantity) {
        return ReturnItem.builder()
                .id(new ReturnItemId(
                        order.getOrderId(),
                        detail.getVariant().getVariantId(),
                        detail.getColor().getColorId()))
                .order(order)
                .variant(detail.getVariant())
                .color(detail.getColor())
                .quantity(quantity)
                .damagedQuantity(0)
                .build();
    }

    private OrderDetail findOrderDetail(List<OrderDetail> orderDetails, String variantId, String colorId) {
        return orderDetails.stream()
                .filter(detail -> detail.getVariant().getVariantId().equals(variantId)
                        && detail.getColor().getColorId().equals(colorId))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Sản phẩm không thuộc đơn hàng"));
    }


    private void validateFiles(List<MultipartFile> videos, List<MultipartFile> images) {
        if (videos != null) {
            for (MultipartFile video : videos) {
                if (!hasFile(video)) {
                    continue;
                }
                if (video.getSize() >= MAX_VIDEO_SIZE) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Mỗi video phải có dung lượng nhỏ hơn 100MB");
                }
                if (!isAllowedVideoType(video.getContentType())) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Video chỉ chấp nhận định dạng MP4 hoặc MOV");
                }
            }
        }

        if (images != null) {
            for (MultipartFile image : images) {
                if (!hasFile(image)) {
                    continue;
                }
                if (image.getSize() > MAX_IMAGE_SIZE) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Mỗi ảnh không được vượt quá 5MB");
                }
                if (!isAllowedImageType(image.getContentType())) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Ảnh chỉ chấp nhận định dạng JPG, PNG hoặc WEBP");
                }
            }
        }
    }

    private int calculateReturnShippingCost(Order order, ReturnReason reason) {
        if (reason == ReturnReason.NOT_RECEIVED) {
            return 0;
        }

        ShipCost shipCost = shipCostRepository.findByCityIgnoreCase(order.getReceiverCity())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phí vận chuyển của tỉnh/thành người nhận"
                ));

        return shipCost.getPriceShip();
    }


    private int calculateRefundAmount(
            Order order,
            List<OrderDetail> orderDetails,
            List<ReturnItem> returnItems,
            ReturnReason reason
    ) {
        if (reason == ReturnReason.NOT_RECEIVED) {
            return order.getTotalAmount();
        }

        long productTotal = order.getProductTotal();
        long orderDiscount = order.getOrderDiscount();

        long returnValue = 0;
        for (ReturnItem returnItem : returnItems) {
            OrderDetail purchasedDetail = findOrderDetail(
                    orderDetails,
                    returnItem.getVariant().getVariantId(),
                    returnItem.getColor().getColorId());
            returnValue += (long) returnItem.getQuantity() * purchasedDetail.getUnitPrice();
        }

        // Giảm giá đơn phân bổ theo tỷ lệ giá trị món trả
        long allocatedDiscount = productTotal > 0 ? orderDiscount * returnValue / productTotal : 0;

        long refundAmount = returnValue - allocatedDiscount;

        // Trả hết mọi món thì hoàn cả phí ship đã trả => refundAmount = totalAmount
        if (isReturningAllItems(orderDetails, returnItems)) {
            refundAmount += order.getShippingFee() - order.getShippingDiscount();
        }

        return Math.toIntExact(refundAmount);
    }

    private boolean isReturningAllItems(List<OrderDetail> orderDetails, List<ReturnItem> returnItems) {
        if (orderDetails.size() != returnItems.size()) {
            return false;
        }

        for (OrderDetail detail : orderDetails) {
            boolean fullyReturned = returnItems.stream().anyMatch(item ->
                    item.getVariant().getVariantId().equals(detail.getVariant().getVariantId())
                            && item.getColor().getColorId().equals(detail.getColor().getColorId())
                            && item.getQuantity().equals(detail.getQuantity()));
            if (!fullyReturned) {
                return false;
            }
        }

        return true;
    }

    private List<ReturnEvidence> uploadEvidenceFiles(
            Order order,
            ReturnRequest request,
            List<Map> uploadedFiles
    ) {
        List<ReturnEvidence> evidences = new ArrayList<>();
        String folder = "laven/returns/" + order.getOrderId();
        String idPrefix = "EVD" + LocalDateTime.now().format(EVIDENCE_DATE_FORMAT);

        List<MultipartFile> videos = request.getVideos() == null ? List.of() : request.getVideos();
        List<MultipartFile> images = request.getImages() == null ? List.of() : request.getImages();

        for (MultipartFile video : videos) {
            if (hasFile(video)) {
                evidences.add(uploadOne(order, video, folder + "/videos",
                        idPrefix + String.format("%02d", evidences.size() + 1), uploadedFiles));
            }
        }
        for (MultipartFile image : images) {
            if (hasFile(image)) {
                evidences.add(uploadOne(order, image, folder + "/images",
                        idPrefix + String.format("%02d", evidences.size() + 1), uploadedFiles));
            }
        }

        return evidences;
    }

    private ReturnEvidence uploadOne(
            Order order,
            MultipartFile file,
            String folder,
            String evidenceId,
            List<Map> uploadedFiles
    ) {
        Map uploadResult = cloudinaryUploadService.uploadFile(file, folder);
        uploadedFiles.add(uploadResult);

        String secureUrl = (String) uploadResult.get("secure_url");
        if (isBlank(secureUrl)) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lấy được đường dẫn file từ Cloudinary");
        }

        return ReturnEvidence.builder()
                .returnEvidenceId(evidenceId)
                .order(order)
                .mediaUrl(secureUrl)
                .build();
    }


    private void deleteUploadedFiles(List<Map> uploadedFiles) {
        for (Map file : uploadedFiles) {
            try {
                String publicId = (String) file.get("public_id");
                String resourceType = (String) file.get("resource_type");

                if (publicId != null && resourceType != null) {
                    cloudinaryUploadService.deleteFile(publicId, resourceType);
                }
            } catch (Exception ignored) {
                // Không ghi đè lỗi gốc bằng lỗi xóa file
            }
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isAllowedImageType(String contentType) {
        return contentType != null && (
                contentType.equalsIgnoreCase("image/jpeg")
                        || contentType.equalsIgnoreCase("image/png")
                        || contentType.equalsIgnoreCase("image/webp"));
    }

    private boolean isAllowedVideoType(String contentType) {
        return contentType != null && (
                contentType.equalsIgnoreCase("video/mp4")
                        || contentType.equalsIgnoreCase("video/quicktime"));
    }

}
