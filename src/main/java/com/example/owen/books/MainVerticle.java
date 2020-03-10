package com.example.owen.books;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {
  private InMemoryBookStore store = new InMemoryBookStore();

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    //Initialize Router
    Router books = Router.router(vertx);
    books.route().handler(BodyHandler.create());

    //GET /books
    books.get("/books").handler(req -> {
      System.out.println("Printing list of books");
      req.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .end(store.getAll().encode());
    });

    //POST /books
    books.post("/books").handler(req -> {
      //get the body
      final JsonObject requestBody = req.getBodyAsJson();
      //store book in memory
      store.add(requestBody.mapTo(Book.class));
      req.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .setStatusCode(HttpResponseStatus.CREATED.code())
        .end(requestBody.encode());

    });

    //PUT /books/:isbn
    books.put("/books/:isbn").handler(req -> {
      final String isbn = req.pathParam("isbn");
      final JsonObject requestBody = req.getBodyAsJson();
      final Book updatedBook = store.update(isbn, requestBody.mapTo(Book.class));

      req.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .end(JsonObject.mapFrom(updatedBook).encode());
    });

    //GET /books/:isbn
    books.get("/books/:isbn").handler(req -> {
      final String isbn = req.pathParam("isbn");
      final Book book = store.get(isbn);
      if (book == null) {
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
          .end(new JsonObject().put("message", "ISBN does not match any book in the database").encode());
      } else {
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .end(JsonObject.mapFrom(book).encode());
      }
    });

    //DELETE /books/:isbn
    books.delete("/books/:isbn").handler(req -> {
      final String isbn = req.pathParam("isbn");
      final Book deletedBook = store.destroy(isbn);
      if (deletedBook == null) {
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
          .end(new JsonObject().put("message", "ISBN does not match any book in the database").encode());
      } else {
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .end(new JsonObject().put("message", "Book Deleted Successfully").encode());
      }
    });

    //Handle errors
    books.errorHandler(500, event -> {
      System.err.println("Failure: " + event.failure());
      event.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .end(new JsonObject().put("error", event.failure().getMessage()).encode());
    });


    startServer(startFuture, books);
  }

  private void startServer(Future<Void> startFuture, Router books) {
    vertx.createHttpServer().requestHandler(books).listen(8888, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        System.out.println(http.cause().getMessage());
        startFuture.fail(http.cause());
      }
    });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
