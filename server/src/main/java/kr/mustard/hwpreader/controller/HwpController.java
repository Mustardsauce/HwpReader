package kr.mustard.hwpreader.controller;

import kr.mustard.hwpreader.service.HwpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reader")
public class HwpController {

    private final HwpService hwpService;

    @Autowired
    public HwpController(HwpService hwpService) {
        this.hwpService = hwpService;
    }

    /**
     * .hwp 또는 .hwpx 파일을 업로드받아 텍스트/표 데이터를 추출하는 API
     */
    @PostMapping("/extract")
    public ResponseEntity<?> extractContent(@RequestParam("file") MultipartFile file) {
        try {
            String result = hwpService.extract(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("처리 중 오류 발생: " + e.getMessage());
        }
    }
}
