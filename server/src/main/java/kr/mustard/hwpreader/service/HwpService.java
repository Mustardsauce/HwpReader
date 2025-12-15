package kr.mustard.hwpreader.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;

import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;

@Service
public class HwpService {    

    /**
     * .hwp 또는 .hwpx 파일에서 텍스트 추출
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

            HWPXFile hwpxFile;
            
            if (isHwpx) {
                // hwpx: File 객체 필요 (HWPXReader가 InputStream 미지원)
                tempFile = File.createTempFile("upload-", ".hwpx");
                try (InputStream is = file.getInputStream();
                     FileOutputStream fos = new FileOutputStream(tempFile)) {
                    is.transferTo(fos);
                }
                hwpxFile = HWPXReader.fromFile(tempFile);
            } else {
                // hwp: InputStream으로 직접 읽기 (임시 파일 불필요)
                hwpxFile = Hwp2Hwpx.toHWPX(HWPReader.fromInputStream(file.getInputStream()));
            }

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

    /**
     * .hwp 또는 .hwpx 파일을 받아서 hwpx 바이너리 데이터를 반환
     * - hwp 파일인 경우: hwpx로 변환 후 반환
     * - hwpx 파일인 경우: 정규화하여 반환
     */
    public byte[] convertToHwpxBinary(MultipartFile file) throws Exception {
        File tempFile = null;
        
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.contains(".")) {
                throw new IllegalArgumentException("Invalid filename.");
            }

            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

            // hwp 또는 hwpx 파일만 허용
            if (!ext.equals("hwp") && !ext.equals("hwpx")) {
                throw new IllegalArgumentException("Only .hwp and .hwpx files are supported.");
            }

            HWPXFile hwpxFile;
            
            if (ext.equals("hwp")) {
                // hwp: InputStream으로 직접 읽기 (임시 파일 불필요)
                hwpxFile = Hwp2Hwpx.toHWPX(HWPReader.fromInputStream(file.getInputStream()));
            } else {
                // hwpx: File 객체 필요 (HWPXReader가 InputStream 미지원)
                tempFile = File.createTempFile("upload-", ".hwpx");
                try (InputStream is = file.getInputStream();
                     FileOutputStream fos = new FileOutputStream(tempFile)) {
                    is.transferTo(fos);
                }
                hwpxFile = HWPXReader.fromFile(tempFile);
            }

            // HWPXFile 객체를 바이트 배열로 변환 (통일된 방식)
            return HWPXWriter.toBytes(hwpxFile);

        } finally {
            // 임시 파일 정리
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    tempFile.deleteOnExit();
                }
            }
        }
    }
}
