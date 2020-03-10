package com.example.owen.books;

public class Book {
  private long isbn;
  private String title;

  public Book(){
    //default constructor
  }

  public Book(final long isbn, final String title) {
    this.isbn = isbn;
    this.title = title;
  }

  public long getIsbn() {
    return isbn;
  }

  public String getTitle() {
    return title;
  }

  public void setIsbn(final long isbn) {
    this.isbn = isbn;
  }

  public void setTitle(final String title) {
    this.title = title;
  }
}
