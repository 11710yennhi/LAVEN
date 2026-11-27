package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.ProductImportRequest.Row;
import iuh.fit.laven.dto.response.ProductImportResponse.Preview;
import iuh.fit.laven.dto.response.ProductImportResponse.Problem;
import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Service
public class ProductImportFileService {
    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 50;
    private static final String SHEET = "SanPham";
    private static final List<String> COLUMNS = List.of(
        "productName", "categoryId", "brand", "origin", "material", "description",
        "waterResistance", "wheel", "zipper", "warranty", "maxQuantityPerOrder", "status", "stopSellingReason"
    );

    public record Template(String name, String contentType, byte[] content) {}

    public Template template(String format) {
        if ("csv".equals(format)) {
            String text = "\uFEFF" + String.join(",", COLUMNS) + "\r\n";
            return new Template("laven-nhap-san-pham.csv", "text/csv;charset=UTF-8", text.getBytes(StandardCharsets.UTF_8));
        }
        if (!"xlsx".equals(format)) {
            throw invalid("Chỉ hỗ trợ mẫu Excel .xlsx và CSV");
        }
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(SHEET);
            sheet.createFreezePane(0, 1);
            var header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.size(); i++) {
                header.createCell(i).setCellValue(COLUMNS.get(i));
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(output);
            return new Template("laven-nhap-san-pham.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Không tạo được mẫu Excel", ex);
        }
    }

    public Preview parse(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            throw invalid("Chọn file có dữ liệu, dung lượng tối đa 2 MB");
        }
        String name = String.valueOf(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        var rows = new ArrayList<Row>();
        var errors = new ArrayList<Problem>();
        try {
            if (name.endsWith(".csv")) {
                readCsv(file.getBytes(), rows, errors);
            } else if (name.endsWith(".xlsx")) {
                readExcel(file.getBytes(), rows, errors);
            } else {
                throw invalid("Chỉ hỗ trợ Excel .xlsx và CSV UTF-8");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalid("Không đọc được file. Hãy sử dụng mẫu Excel .xlsx hoặc CSV UTF-8 của LAVEN");
        }
        if (rows.isEmpty() && errors.isEmpty()) {
            throw invalid("File chưa có dòng sản phẩm");
        }
        return new Preview(List.copyOf(rows), List.copyOf(errors));
    }

    private void readCsv(byte[] bytes, List<Row> rows, List<Problem> errors) throws IOException {
        String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        CSVFormat format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(false).get();
        try (var parser = format.parse(new java.io.StringReader(text))) {
            var records = parser.iterator();
            if (!records.hasNext()) throw invalid("File chưa có dòng tiêu đề");
            List<String> headers = records.next().toList();
            checkHeaders(headers);
            int line = (int) parser.getCurrentLineNumber() + 1;
            while (records.hasNext()) {
                List<String> values = records.next().toList();
                addRow(line, headers, values, rows, errors);
                line = (int) parser.getCurrentLineNumber() + 1;
            }
        }
    }

    private void readExcel(byte[] bytes, List<Row> rows, List<Problem> errors) throws IOException {
        checkExcelSize(bytes);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet(SHEET);
            if (sheet == null || sheet.getRow(0) == null) {
                throw invalid("Excel phải có sheet SanPham và tiêu đề ở dòng 1");
            }
            var formatter = new DataFormatter(Locale.ROOT);
            var header = sheet.getRow(0);
            var headers = new ArrayList<String>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                headers.add(formatter.formatCellValue(header.getCell(c)));
            }
            checkHeaders(headers);
            for (var row : sheet) {
                if (row.getRowNum() == 0) continue;
                int line = row.getRowNum() + 1;
                var values = new ArrayList<String>();
                for (int c = 0; c < headers.size(); c++) {
                    var cell = row.getCell(c);
                    if (cell != null && (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR)) {
                        errors.add(new Problem(line, headers.get(c), "Hãy nhập giá trị trực tiếp vào ô"));
                        values.add("");
                    } else {
                        values.add(formatter.formatCellValue(cell));
                    }
                }
                for (var cell : row) {
                    if (cell.getColumnIndex() >= headers.size() && cell.getCellType() != CellType.BLANK) {
                        errors.add(new Problem(line, "File", "Có ô dữ liệu nằm ngoài cột của mẫu"));
                        break;
                    }
                }
                addRow(line, headers, values, rows, errors);
            }
        }
    }

    private void checkExcelSize(byte[] bytes) throws IOException {
        // Giới hạn dung lượng sau giải nén trước khi chuyển workbook cho thư viện Excel.
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[8192];
            long expanded = 0;
            int entries = 0;
            while (zip.getNextEntry() != null) {
                if (++entries > 200) throw invalid("File Excel có quá nhiều thành phần");
                int size;
                while ((size = zip.read(buffer)) != -1) {
                    expanded += size;
                    if (expanded > 24L * 1024 * 1024) throw invalid("File Excel quá lớn sau giải nén");
                }
            }
        }
    }

    private void checkHeaders(List<String> headers) {
        var normalized = headers.stream().map(String::trim).toList();
        for (String header : normalized) {
            if (!COLUMNS.contains(header)) throw invalid("Cột không thuộc mẫu sản phẩm: " + header);
        }
        if (normalized.stream().distinct().count() != normalized.size()) throw invalid("File có tên cột bị trùng");
        for (String required : COLUMNS.subList(0, 5)) {
            if (!normalized.contains(required)) throw invalid("Thiếu cột " + required + " ở dòng 1");
        }
    }

    private void addRow(int line, List<String> headers, List<String> cells, List<Row> rows, List<Problem> errors) {
        if (cells.stream().allMatch(String::isBlank)) return;
        if (rows.size() >= MAX_ROWS) throw invalid("Mỗi file chỉ được nhập tối đa 50 sản phẩm");
        if (cells.size() > headers.size()) errors.add(new Problem(line, "File", "Dòng có nhiều ô hơn tiêu đề"));
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < Math.min(headers.size(), cells.size()); i++) {
            String text = cells.get(i).strip();
            if (!text.isBlank()) data.put(headers.get(i).trim(), text);
        }
        Integer limit = 5;
        if (data.containsKey("maxQuantityPerOrder")) {
            try {
                limit = new BigDecimal(data.get("maxQuantityPerOrder")).intValueExact();
            } catch (IllegalArgumentException | ArithmeticException ex) {
                errors.add(new Problem(line, "maxQuantityPerOrder", "Giới hạn mua phải là số nguyên"));
            }
        }
        String status = data.getOrDefault("status", "true");
        if (!status.equalsIgnoreCase("true") && !status.equalsIgnoreCase("false")) {
            errors.add(new Problem(line, "status", "Nhập true hoặc false"));
        }
        rows.add(new Row(line, data.get("productName"), data.get("categoryId"), data.get("brand"),
            data.get("origin"), data.get("material"), data.get("description"), data.get("waterResistance"),
            data.get("wheel"), data.get("zipper"), data.get("warranty"), limit, Boolean.parseBoolean(status),
            data.get("stopSellingReason")));
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
