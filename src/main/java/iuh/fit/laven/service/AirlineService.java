package iuh.fit.laven.service;

import iuh.fit.laven.model.Airline;
import iuh.fit.laven.model.BaggageRule;
import iuh.fit.laven.model.enums.BaggageType;
import iuh.fit.laven.repository.AirlineRepository;
import iuh.fit.laven.repository.BaggageRuleRepository;
import iuh.fit.laven.util.IdGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Quản lý hãng bay và quy định hành lý (admin), dữ liệu cho chatbot tư vấn. */
@Service
@Transactional
public class AirlineService {
    private final AirlineRepository airlineRepository;
    private final BaggageRuleRepository baggageRuleRepository;

    public AirlineService(AirlineRepository airlineRepository, BaggageRuleRepository baggageRuleRepository) {
        this.airlineRepository = airlineRepository;
        this.baggageRuleRepository = baggageRuleRepository;
    }

    // ===================== Hãng bay =====================

    @Transactional(readOnly = true)
    public List<Airline> findActive() {
        return airlineRepository.findByStatusTrueOrderByAirlineNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Airline> search(String airlineName, Boolean status, LocalDate verifiedFrom, LocalDate verifiedTo) {
        validateDateRange(verifiedFrom, verifiedTo);
        return airlineRepository.search(blank(airlineName), status, verifiedFrom, verifiedTo);
    }

    @Transactional(readOnly = true)
    public Airline findById(String airlineId) {
        return airlineRepository.findById(airlineId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hãng bay không tồn tại"));
    }

    public Airline create(Airline request) {
        validateAirline(request);
        if (airlineRepository.existsByAirlineNameIgnoreCase(request.getAirlineName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng bay đã tồn tại");
        }
        request.setAirlineId(IdGenerator.nextUnique("AIR", airlineRepository::existsById));
        return airlineRepository.save(request);
    }

    /** Sửa thông tin và trạng thái (status) của hãng. */
    public Airline update(String airlineId, Airline request) {
        Airline airline = findById(airlineId);
        validateAirline(request);
        if (airlineRepository.existsByAirlineNameIgnoreCaseAndAirlineIdNot(request.getAirlineName(), airlineId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng bay đã tồn tại");
        }
        airline.setAirlineName(request.getAirlineName());
        airline.setBaggagePolicyUrl(request.getBaggagePolicyUrl());
        airline.setVerifiedAt(request.getVerifiedAt());
        airline.setStatus(request.isStatus());
        return airlineRepository.save(airline);
    }

    /** Không xóa thật: ngừng dùng hãng, giữ lại quy định để bật lại sau. */
    public Airline deactivate(String airlineId) {
        Airline airline = findById(airlineId);
        airline.setStatus(false);
        return airlineRepository.save(airline);
    }

    // ===================== Quy định hành lý =====================

    /** Khách xem quy định của hãng đang dùng. */
    @Transactional(readOnly = true)
    public List<BaggageRule> findRulesOfActiveAirline(String airlineId) {
        Airline airline = findById(airlineId);
        if (!airline.isStatus()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hãng bay không tồn tại");
        }
        return baggageRuleRepository.findByAirlineAirlineIdOrderByBaggageTypeAscFareClassAsc(airlineId);
    }

    @Transactional(readOnly = true)
    public List<BaggageRule> searchRules(String airlineId, String airlineName, BaggageType baggageType,
                                         String fareClass, Boolean airlineStatus,
                                         LocalDate verifiedFrom, LocalDate verifiedTo) {
        validateDateRange(verifiedFrom, verifiedTo);
        return baggageRuleRepository.search(blank(airlineId), blank(airlineName), baggageType,
            blank(fareClass), airlineStatus, verifiedFrom, verifiedTo);
    }

    @Transactional(readOnly = true)
    public BaggageRule findRuleById(String baggageRuleId) {
        return baggageRuleRepository.findById(baggageRuleId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quy định hành lý không tồn tại"));
    }

    public BaggageRule createRule(String airlineId, BaggageRule request) {
        Airline airline = findById(airlineId);
        if (!airline.isStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thêm quy định cho hãng đã ngừng");
        }
        validateRule(request);
        if (baggageRuleRepository.existsByAirlineAirlineIdAndBaggageTypeAndFareClassIgnoreCase(
                airlineId, request.getBaggageType(), request.getFareClass())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hãng đã có quy định cho loại hành lý và hạng vé này");
        }
        request.setBaggageRuleId(IdGenerator.nextUnique("BGR", baggageRuleRepository::existsById));
        request.setAirline(airline);
        return baggageRuleRepository.save(request);
    }

    /** Không đổi hãng của quy định (airline chỉ đọc trong JSON nên body không đổi được). */
    public BaggageRule updateRule(String baggageRuleId, BaggageRule request) {
        BaggageRule rule = findRuleById(baggageRuleId);
        validateRule(request);
        if (baggageRuleRepository.existsByAirlineAirlineIdAndBaggageTypeAndFareClassIgnoreCaseAndBaggageRuleIdNot(
                rule.getAirline().getAirlineId(), request.getBaggageType(), request.getFareClass(), baggageRuleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hãng đã có quy định cho loại hành lý và hạng vé này");
        }
        rule.setBaggageType(request.getBaggageType());
        rule.setFareClass(request.getFareClass());
        rule.setPieceCount(request.getPieceCount());
        rule.setMaxWeight(request.getMaxWeight());
        rule.setMaxLength(request.getMaxLength());
        rule.setMaxWidth(request.getMaxWidth());
        rule.setMaxHeight(request.getMaxHeight());
        rule.setMaxTotalDimension(request.getMaxTotalDimension());
        rule.setNote(request.getNote());
        return baggageRuleRepository.save(rule);
    }

    /** Quy định không có bảng nào tham chiếu nên xóa thật. */
    public void deleteRule(String baggageRuleId) {
        baggageRuleRepository.delete(findRuleById(baggageRuleId));
    }

    // ===================== Kiểm tra dữ liệu =====================

    private void validateAirline(Airline request) {
        request.setAirlineName(request.getAirlineName().trim());
        String url = request.getBaggagePolicyUrl().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link chính sách phải bắt đầu bằng http:// hoặc https://");
        }
        request.setBaggagePolicyUrl(url);
        if (request.getVerifiedAt().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kiểm tra không được ở tương lai");
        }
    }

    private void validateRule(BaggageRule request) {
        request.setFareClass(request.getFareClass().trim());
        request.setNote(blank(request.getNote()));
        int dimensionCount = (request.getMaxLength() == null ? 0 : 1)
            + (request.getMaxWidth() == null ? 0 : 1)
            + (request.getMaxHeight() == null ? 0 : 1);
        if (dimensionCount != 0 && dimensionCount != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhập đủ dài, rộng, cao hoặc bỏ trống cả 3");
        }
        if (dimensionCount == 0 && request.getMaxTotalDimension() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần nhập kích thước 3 chiều hoặc tổng 3 chiều tối đa");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu phải trước ngày kết thúc");
        }
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
