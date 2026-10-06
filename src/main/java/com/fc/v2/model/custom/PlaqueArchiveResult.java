package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 老化归档结果：结论长期停在待核的行，连着满两个年度，转入历史卷宗层。
 *
 * <p>规矩只对历史册生效，新册走不到它面前；归档逐行挪卷宗层，行不删、不换册，
 * 存量此前入账的一律照常可查、一条不减，只是现行名单不再列出。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class PlaqueArchiveResult {

    /** 一本册里被挪进历史卷宗层的行 */
    public static class BookArchive {
        private String batchNo;
        private List<Integer> rowNos = new ArrayList<Integer>();

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public List<Integer> getRowNos() {
            return rowNos;
        }

        public void setRowNos(List<Integer> rowNos) {
            this.rowNos = rowNos;
        }
    }

    /** 扫描册数 */
    private int scannedBooks;
    /** 挪进历史卷宗层的行数 */
    private int archivedRows;
    /** 判定所用：截止年度（含），待核行建行年度与它差满两个年度才挪 */
    private int asOfYear;
    private List<BookArchive> books = new ArrayList<BookArchive>();

    public int getScannedBooks() {
        return scannedBooks;
    }

    public void setScannedBooks(int scannedBooks) {
        this.scannedBooks = scannedBooks;
    }

    public int getArchivedRows() {
        return archivedRows;
    }

    public void setArchivedRows(int archivedRows) {
        this.archivedRows = archivedRows;
    }

    public int getAsOfYear() {
        return asOfYear;
    }

    public void setAsOfYear(int asOfYear) {
        this.asOfYear = asOfYear;
    }

    public List<BookArchive> getBooks() {
        return books;
    }

    public void setBooks(List<BookArchive> books) {
        this.books = books;
    }
}
