package com.ctt535.start.shambook;

import android.graphics.Bitmap;

public class BookInformation {
    private int id;
    private String name;
    private String authors;
    private String format;
    private Bitmap coverImage;
    private String filePath;
    private int totalPage;
    private int precentRead;
    private int currentPage;
    private int dateRead;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Bitmap getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(Bitmap coverImage) {
        this.coverImage = coverImage;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getPrecentRead() {
        return precentRead;
    }

    public void setPrecentRead(int precentRead) {
        this.precentRead = precentRead;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getDateRead() {
        return dateRead;
    }

    public void setDateRead(int dateRead) {
        this.dateRead = dateRead;
    }

    @Override
    public String toString() {
        return "BookInformation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", authors='" + authors + '\'' +
                ", format='" + format + '\'' +
                ", coverImage=" + coverImage +
                ", filePath='" + filePath + '\'' +
                ", totalPage=" + totalPage +
                ", precentRead=" + precentRead +
                ", currentPage=" + currentPage +
                ", dateRead=" + dateRead +
                '}';
    }
}
