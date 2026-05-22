package com.mhd.interview.web.modules.base;

/**
 * 导出结果
 *
 * @author zhao-hao-dong
 */
public record ExportResult(byte[] pdfBytes, String filename) {
}
