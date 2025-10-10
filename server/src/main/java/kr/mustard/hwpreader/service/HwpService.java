package kr.mustard.hwpreader.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;

import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;

@Service
public class HwpService {    

    /**
     * .hwpx 파일에서 텍스트 추출
     */
    public String extract(MultipartFile file) throws Exception {

        File tempFile = null;
        try {

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.contains("."))
            {
                throw new IllegalArgumentException("Filename is invalid.");
            }

            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

            boolean isHwpx;
            if (ext.equals("hwpx"))
            {
                isHwpx = true;
            }
            else if (ext.equals("hwp"))
            {
                isHwpx = false;
            }
            else
            {
                throw new IllegalArgumentException("This file format is not supported.");
            }
            
            String tempExt = isHwpx ? ".hwpx" : ".hwp";

            // HWPXReader에는 fromInputStream이 없으므로 임시 파일 생성
            tempFile = File.createTempFile("upload-", tempExt);
            try (InputStream is = file.getInputStream();
                FileOutputStream fos = new FileOutputStream(tempFile)) {
                is.transferTo(fos);
            }

            // HWPX 파일 로드
            HWPXFile hwpxFile = isHwpx
                            ? HWPXReader.fromFile(tempFile) 
                            : Hwp2Hwpx.toHWPX(HWPReader.fromFile(tempFile));

            // 텍스트 구분자 설정
            // (표 시작/끝, 행/열 구분을 명시적으로 추가)
            TextMarks marks = new TextMarks()
                        .lineBreakAnd("\n")
                        .paraSeparatorAnd("\n")
                        .tableStartAnd("\n<TABLE_START>\n")
                        .tableEndAnd("\n<TABLE_END>\n")
                        .tableRowSeparatorAnd(" <ROW_END>\n<ROW_START>")            // ✅ 행 구분자
                        .tableCellSeparatorAnd(" | ")                     // ✅ 셀 구분자
                        .fieldStartAnd("<FIELD_START>")                          // ✅ 셀 시작
                        .fieldEndAnd("<FIELD_END>")                           // ✅ 셀 끝
                        .containerStartAnd("- ")                          // ✅ 리스트 항목 시작
                        .containerEndAnd("\n");                           // ✅ 리스트 항목 종료


            // 텍스트 추출
            String result = TextExtractor.extract(
                    hwpxFile,
                    TextExtractMethod.InsertControlTextBetweenParagraphText,
                    true,
                    marks
            );

            // 후처리: 불필요한 공백 정리
            if (result != null) {
                result = result                        
                        .replaceAll("(<TABLE_START>\n)", "$1<ROW_START> ")
                        .replaceAll("(\n<TABLE_END>)", " <ROW_END>$1")
                        .replaceAll("\\n{3,}", "\n\n")      // 3줄 이상 공백 제거
                        .replaceAll(" {2,}", " ")           // 공백 2개 이상을 하나로
                        .trim();
            }

            return result != null ? result.trim() : "";
            
        } finally {       
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    tempFile.deleteOnExit();
                }
            }
        }
    }
}
