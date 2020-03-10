package com.example.owen.books;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class InMemoryBookStore {
  private Map<Long, Book> books = new HashMap<>();

  InMemoryBookStore() {
    books.put(1L, new Book(1L, "Alice in Wonderland"));
    books.put(2L, new Book(2L, "Alice in Zombieland"));
    books.put(3L, new Book(3L, "Alice in Jumanji"));
  }

  // Get all books
  public JsonArray getAll() {
    JsonArray all = new JsonArray();
    books.values().forEach(book -> {
      all.add(JsonObject.mapFrom(book));
    });
    return all;
  }

  //Add new book
  public void add(final Book entry) {
    books.put(entry.getIsbn(), entry);
  }

  //Update a book
  public Book update(final String isbn, final Book entry) {
    Long key = Long.parseLong(isbn);
    if (key != entry.getIsbn()) {
      throw new IllegalArgumentException("ISBN does not match book in database");
    }
    books.put(key, entry);
    return entry;
  }

  //Get a specific book
  public Book get(final String isbn) {
    Long key = Long.parseLong(isbn);
    return books.get(key);
  }

  //Delete a book
  public Book destroy(String isbn) {
    Long key = Long.parseLong(isbn);
    return books.remove(key);
  }
}
