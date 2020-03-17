package com.example.owen.books;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;

import java.util.List;

public class JDBCBookRepository {
  private SQLClient sql;

  public JDBCBookRepository(final Vertx vertx) {

    final JsonObject config = new JsonObject();
    config.put("url", "jdbc:postgresql://127.0.0.1/books");
    config.put("driver_class", "org.postgresql.Driver");
    config.put("user", "postgres");
    config.put("password", "secret");
    sql = JDBCClient.createShared(vertx, config);
  }

  //Get All Books
  public Future<JsonArray> getAll() {
    final Future<JsonArray> getAll = Future.future();
    sql.query("SELECT * FROM books", ar -> {
      if (ar.failed()) {
        getAll.fail(ar.cause());
        return;
      }
      final List<JsonObject> rows = ar.result().getRows();
      final JsonArray result = new JsonArray();
      rows.forEach(result::add);
      getAll.complete(result);
    });

    return getAll;
  }

  //Create A book
  public Future<Void> create(final Book bookToAdd) {
    final Future<Void> added = Future.future();
    final JsonArray params = new JsonArray().add(bookToAdd.getIsbn()).add(bookToAdd.getTitle());
    sql.updateWithParams("INSERT INTO books (isbn, title) VALUES (?,?)", params, ar -> {
      if (ar.failed()) {
        //forward error
        added.fail(ar.cause());
        return;
      }
      //return error when udpdate count is not 1
      if (ar.result().getUpdated() != 1) {
        added.fail(new IllegalStateException("Repo not updated"));
      }

      added.complete();
    });
    return added;
  }

  public Future<String> delete(String isbn) {
    int id = Integer.parseInt(isbn);
    final JsonArray params = new JsonArray().add(id);
    final Future<String> toBeDeleted = Future.future();
    sql.updateWithParams("DELETE FROM books WHERE books.isbn = ?", params, ar -> {
      if (ar.failed()) {
        //forward error
        toBeDeleted.fail(ar.cause());
        return;
      }
      //return error when update count is not 1
      if (ar.result().getUpdated() == 0) {
        toBeDeleted.complete();
        return;
      }
      toBeDeleted.complete(isbn);
    });
    return toBeDeleted;
  }

  public Future<Void> update(String isbn, Book bookToUpdate) {
    int id = Integer.parseInt(isbn);
    final Future<Void> updated = Future.future();
    final JsonArray params = new JsonArray().add(bookToUpdate.getTitle()).add(id);
    sql.updateWithParams("UPDATE books SET title = ? WHERE isbn = ?", params, ar -> {
      if (ar.failed()) {
        //forward error
        updated.fail(ar.cause());
        return;
      }
      //return error when update count is not 1
      if (ar.result().getUpdated() != 1) {
        updated.fail(new IllegalStateException("Repo not updated"));
      }
      updated.complete();
    });
    return updated;

  }
}
