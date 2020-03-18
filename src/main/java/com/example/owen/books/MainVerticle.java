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
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);
  private InMemoryBookStore store = new InMemoryBookStore();
  private JDBCBookRepository booksRepo;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    booksRepo = new JDBCBookRepository(vertx); //init db
    //Initialize Router
    Router books = Router.router(vertx);
    books.route().handler(BodyHandler.create());

    //todo extract endpoints to separate controller
    //GET /books
    all(books);
    //POST /books
    store(books);
    //PUT /books/:isbn
    update(books);
    //GET /books/:isbn
    show(books);
    //DELETE /books/:isbn
    destroy(books);
    //Handle errors
    handleErrors(books);

    startServer(startFuture, books);
  }

  private void handleErrors(Router books) {
    books.errorHandler(500, event -> {
      LOG.error("Failure", event.failure());
      event.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .end(new JsonObject().put("error", event.failure().getMessage()).encode());
    });
  }

  private void destroy(final Router books) {
    books.delete("/books/:isbn").handler(req -> {
      final String isbn = req.pathParam("isbn");
      booksRepo.delete(isbn).setHandler(ar -> {
        if (ar.failed()) {
          req.fail(ar.cause());
          return;
        }
        if (ar.result() == null) {
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

    });
  }

  private void show(Router books) {
    books.get("/books/:isbn").handler(req -> {
      final String isbn = req.pathParam("isbn");
      booksRepo.getOne(isbn).setHandler(ar -> {
        if (ar.failed()) {
          req.fail(ar.cause());
          return;
        }
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(HttpResponseStatus.CREATED.code())
          .end(ar.result().encode());
      });
    });

  }

  private void update(Router books) {
    books.put("/books/:isbn").handler(req -> {
      final String isbn = req.pathParam("isbn");
      final JsonObject requestBody = req.getBodyAsJson();
      booksRepo.update(isbn, requestBody.mapTo(Book.class)).setHandler(ar -> {
        if (ar.failed()) {
          req.fail(ar.cause());
          return;
        }
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(HttpResponseStatus.CREATED.code())
          .end(requestBody.encode());
      });
    });
  }

  private void store(Router books) {
    books.post("/books").handler(req -> {
      //get the body
      final JsonObject requestBody = req.getBodyAsJson();
      //store book in memory
      booksRepo.create(requestBody.mapTo(Book.class)).setHandler(ar -> {
        if (ar.failed()) {
          req.fail(ar.cause());
          return;
        }
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(HttpResponseStatus.CREATED.code())
          .end(requestBody.encode());

      });
    });

  }

  private void all(Router books) {
    books.get("/books").handler(req -> {
      LOG.info("Printing list of books");
      booksRepo.getAll().setHandler(ar -> {
        if (ar.failed()) {
          //forward call
          req.fail(ar.cause());
          return;
        }
        req.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .end(ar.result().encode());
      });
    });

  }

  private void startServer(Future<Void> startFuture, Router books) {
    LOG.info("Booting up server....");
    vertx.createHttpServer().requestHandler(books).listen(8888, http -> {
      if (http.succeeded()) {
        startFuture.complete();
        LOG.info("HTTP server started on port 8888");
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
