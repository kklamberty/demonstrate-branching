package umm3601.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Tests the logic of the UserController
 *
 * @throws IOException
 */
// The tests here include a ton of "magic numbers" (numeric constants).
// It wasn't clear to me that giving all of them names would actually
// help things. The fact that it wasn't obvious what to call some
// of them says a lot. Maybe what this ultimately means is that
// these tests can/should be restructured so the constants (there are
// also a lot of "magic strings" that Checkstyle doesn't actually
// flag as a problem) make more sense.
@SuppressWarnings({ "MagicNumber" })
class UserControllerSpec {

  // An instance of the controller we're testing that is prepared in
  // `setupEach()`, and then exercised in the various tests below.
  private UserController userController;

  // A Mongo object ID that is initialized in `setupEach()` and used
  // in a few of the tests. It isn't used all that often, though,
  // which suggests that maybe we should extract the tests that
  // care about it into their own spec file?
  private ObjectId samsId;

  // The client and database that will be used
  // for all the tests in this spec file.
  private static MongoClient mongoClient;
  private static MongoDatabase db;

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<User>> userArrayListCaptor;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  /**
   * Sets up (the connection to the) DB once; that connection and DB will
   * then be (re)used for all the tests, and closed in the `teardown()`
   * method. It's somewhat expensive to establish a connection to the
   * database, and there are usually limits to how many connections
   * a database will support at once. Limiting ourselves to a single
   * connection that will be shared across all the tests in this spec
   * file helps both speed things up and reduce the load on the DB
   * engine.
   */
  @BeforeAll
  static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build());
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() throws IOException {
    // Reset our mock context and argument captor (declared with Mockito
    // annotations @Mock and @Captor)
    MockitoAnnotations.openMocks(this);

    // Setup database
    MongoCollection<Document> userDocuments = db.getCollection("users");
    userDocuments.drop();
    List<Document> testUsers = new ArrayList<>();
    testUsers.add(
        new Document()
            .append("name", "Chris")
            .append("age", 25)
            .append("company", "UMM")
            .append("email", "chris@this.that")
            .append("role", "admin")
            .append("avatar", "https://gravatar.com/avatar/8c9616d6cc5de638ea6920fb5d65fc6c?d=identicon"));
    testUsers.add(
        new Document()
            .append("name", "Pat")
            .append("age", 37)
            .append("company", "IBM")
            .append("email", "pat@something.com")
            .append("role", "editor")
            .append("avatar", "https://gravatar.com/avatar/b42a11826c3bde672bce7e06ad729d44?d=identicon"));
    testUsers.add(
        new Document()
            .append("name", "Jamie")
            .append("age", 37)
            .append("company", "OHMNET")
            .append("email", "jamie@frogs.com")
            .append("role", "viewer")
            .append("avatar", "https://gravatar.com/avatar/d4a6c71dd9470ad4cf58f78c100258bf?d=identicon"));

    samsId = new ObjectId();
    Document sam = new Document()
        .append("_id", samsId)
        .append("name", "Sam")
        .append("age", 45)
        .append("company", "OHMNET")
        .append("email", "sam@frogs.com")
        .append("role", "viewer")
        .append("avatar", "https://gravatar.com/avatar/08b7610b558a4cbbd20ae99072801f4d?d=identicon");

    userDocuments.insertMany(testUsers);
    userDocuments.insertOne(sam);

    userController = new UserController(db);
  }

  @Test
  void addsRoutes() {
    Javalin mockServer = mock(Javalin.class);
    userController.addRoutes(mockServer);
    verify(mockServer, Mockito.atLeast(1)).get(any(), any());
  }

  @Test
  void canGetAllUsers() throws IOException {
    // When something asks the (mocked) context for the queryParamMap,
    // it will return an empty map (since there are no query params in
    // this case where we want all users).
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    // Now, go ahead and ask the userController to getUsers
    // (which will, indeed, ask the context for its queryParamMap)
    userController.getUsers(ctx);

    // We are going to capture an argument to a function, and the type of
    // that argument will be of type ArrayList<User> (we said so earlier
    // using a Mockito annotation like this):
    // @Captor
    // private ArgumentCaptor<ArrayList<User>> userArrayListCaptor;
    // We only want to declare that captor once and let the annotation
    // help us accomplish reassignment of the value for the captor
    // We reset the values of our annotated declarations using the command
    // `MockitoAnnotations.openMocks(this);` in our @BeforeEach

    // Specifically, we want to pay attention to the ArrayList<User> that
    // is passed as input when ctx.json is called --- what is the argument
    // that was passed? We capture it and can refer to it later.
    verify(ctx).json(userArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Check that the database collection holds the same number of documents
    // as the size of the captured List<User>
    assertEquals(
        db.getCollection("users").countDocuments(),
        userArrayListCaptor.getValue().size());
  }

  /**
   * Test that the `generateAvatar` method works as expected.
   *
   * To test this code, we need to mock out the `md5()` method so we
   * can control what it returns. This way we don't have to figure
   * out what the actual md5 hash of a particular email address is.
   *
   * The use of `Mockito.spy()` essentially allows us to override
   * the `md5()` method, while leaving the rest of the user controller
   * "as is". This is a nice way to test a method that depends on
   * an internal method that we don't want to test (`md5()` in this case).
   *
   * This code was suggested by GitHub CoPilot.
   *
   * @throws NoSuchAlgorithmException
   */
  @Test
  void testGenerateAvatar() throws NoSuchAlgorithmException {
    // Arrange
    String email = "test@example.com";
    UserController controller = Mockito.spy(userController);
    when(controller.md5(email)).thenReturn("md5hash");

    // Act
    String avatar = controller.generateAvatar(email);

    // Assert
    assertEquals("https://gravatar.com/avatar/md5hash?d=identicon", avatar);
  }

  /**
   * Test that the `generateAvatar` throws a `NoSuchAlgorithmException`
   * if it can't find the `md5` hashing algorithm.
   *
   * To test this code, we need to mock out the `md5()` method so we
   * can control what it returns. In particular, we want `.md5()` to
   * throw a `NoSuchAlgorithmException`, which we can't do without
   * mocking `.md5()` (since the algorithm does actually exist).
   *
   * The use of `Mockito.spy()` essentially allows us to override
   * the `md5()` method, while leaving the rest of the user controller
   * "as is". This is a nice way to test a method that depends on
   * an internal method that we don't want to test (`md5()` in this case).
   *
   * This code was suggested by GitHub CoPilot.
   *
   * @throws NoSuchAlgorithmException
   */
  @Test
  void testGenerateAvatarWithException() throws NoSuchAlgorithmException {
    // Arrange
    String email = "test@example.com";
    UserController controller = Mockito.spy(userController);
    when(controller.md5(email)).thenThrow(NoSuchAlgorithmException.class);

    // Act
    String avatar = controller.generateAvatar(email);

    // Assert
    assertEquals("https://gravatar.com/avatar/?d=mp", avatar);
  }
}
