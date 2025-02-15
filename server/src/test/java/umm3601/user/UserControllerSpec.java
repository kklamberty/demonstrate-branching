package umm3601.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import io.javalin.validation.Validation;
import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;

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
   * Confirm that if we process a request for users with age 37,
   * that all returned users have that age, and we get the correct
   * number of users.
   *
   * The structure of this test is:
   *
   *    - We create a `Map` for the request's `queryParams`, that
   *      contains a single entry, mapping the `AGE_KEY` to the
   *      target value ("37"). This "tells" our `UserController`
   *      that we want all the `User`s that have age 37.
   *    - We create a validator that confirms that the code
   *      we're testing calls `ctx.queryParamsAsClass("age", Integer.class)`,
   *      i.e., it asks for the value in the query param map
   *      associated with the key `"age"`, interpreted as an Integer.
   *      That call needs to return a value of type `Validator<Integer>`
   *      that will succeed and return the (integer) value `37` associated
   *      with the (`String`) parameter value `"37"`.
   *    - We then call `userController.getUsers(ctx)` to run the code
   *      being tested with the constructed context `ctx`.
   *    - We also use the `userListArrayCaptor` (defined above)
   *      to capture the `ArrayList<User>` that the code under test
   *      passes to `ctx.json(…)`. We can then confirm that the
   *      correct list of users (i.e., all the users with age 37)
   *      is passed in to be returned in the context.
   *    - Now we can use a variety of assertions to confirm that
   *      the code under test did the "right" thing:
   *       - Confirm that the list of users has length 2
   *       - Confirm that each user in the list has age 37
   *       - Confirm that their names are "Jamie" and "Pat"
   *
   * @throws IOException
   */
  @Test
  void canGetUsersWithAge37() throws IOException {
    // We'll need both `String` and `Integer` representations of
    // the target age, so I'm defining both here.
    Integer targetAge = 37;
    String targetAgeString = targetAge.toString();

    // Create a `Map` for the `queryParams` that will "return" the string
    // "37" if you ask for the value associated with the `AGE_KEY`.
    Map<String, List<String>> queryParams = new HashMap<>();

    queryParams.put(UserController.AGE_KEY, Arrays.asList(new String[] {targetAgeString}));
    // When the code being tested calls `ctx.queryParamMap()` return the
    // the `queryParams` map we just built.
    when(ctx.queryParamMap()).thenReturn(queryParams);
    // When the code being tested calls `ctx.queryParam(AGE_KEY)` return the
    // `targetAgeString`.
    when(ctx.queryParam(UserController.AGE_KEY)).thenReturn(targetAgeString);

    // Create a validator that confirms that when we ask for the value associated with
    // `AGE_KEY` _as an integer_, we get back the integer value 37.
    Validation validation = new Validation();
    // The `AGE_KEY` should be name of the key whose value is being validated.
    // You can actually put whatever you want here, because it's only used in the generation
    // of testing error reports, but using the actually key value will make those reports more informative.
    Validator<Integer> validator = validation.validator(UserController.AGE_KEY, Integer.class, targetAgeString);
    // When the code being tested calls `ctx.queryParamAsClass("age", Integer.class)`
    // we'll return the `Validator` we just constructed.
    when(ctx.queryParamAsClass(UserController.AGE_KEY, Integer.class))
        .thenReturn(validator);

    userController.getUsers(ctx);

    // Confirm that the code being tested calls `ctx.json(…)`, and capture whatever
    // is passed in as the argument when `ctx.json()` is called.
    verify(ctx).json(userArrayListCaptor.capture());
    // Confirm that the code under test calls `ctx.status(HttpStatus.OK)` is called.
    verify(ctx).status(HttpStatus.OK);

    // Confirm that we get back two users.
    assertEquals(2, userArrayListCaptor.getValue().size());
    // Confirm that both users have age 37.
    for (User user : userArrayListCaptor.getValue()) {
      assertEquals(targetAge, user.age);
    }
    // Generate a list of the names of the returned users.
    List<String> names = userArrayListCaptor.getValue().stream().map(user -> user.name).collect(Collectors.toList());
    // Confirm that the returned `names` contain the two names of the
    // 37-year-olds.
    assertTrue(names.contains("Jamie"));
    assertTrue(names.contains("Pat"));
  }

  /**
   * Test that if the user sends a request with an illegal value in
   * the age field (i.e., something that can't be parsed to a number)
   * we get a reasonable error back.
   */
  @Test
  void respondsAppropriatelyToNonNumericAge() {
    Map<String, List<String>> queryParams = new HashMap<>();
    String illegalIntegerString = "bad integer string";
    queryParams.put(UserController.AGE_KEY, Arrays.asList(new String[] {illegalIntegerString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    // When the code being tested calls `ctx.queryParam(AGE_KEY)` return the
    // `illegalIntegerString`.
    when(ctx.queryParam(UserController.AGE_KEY)).thenReturn(illegalIntegerString);

    // Create a validator that confirms that when we ask for the value associated with
    // `AGE_KEY` _as an integer_, we get back the `illegalIntegerString`.
    Validation validation = new Validation();
    // The `AGE_KEY` should be name of the key whose value is being validated.
    // You can actually put whatever you want here, because it's only used in the generation
    // of testing error reports, but using the actually key value will make those reports more informative.
    Validator<Integer> validator = validation.validator(UserController.AGE_KEY, Integer.class, illegalIntegerString);
    when(ctx.queryParamAsClass(UserController.AGE_KEY, Integer.class)).thenReturn(validator);

    // This should now throw a `ValidationException` because
    // our request has an age that can't be parsed to a number.
    ValidationException exception = assertThrows(ValidationException.class, () -> {
      userController.getUsers(ctx);
    });
    // This digs into the returned `ValidationException` to get the underlying `Exception` that caused
    // the validation to fail:
    //   - `exception.getErrors` returns a `Map` that maps keys (like `AGE_KEY`) to lists of
    //      validation errors for that key
    //   - `.get(AGE_KEY)` returns a list of all the validation errors associated with `AGE_KEY`
    //   - `.get(0)` assumes that the root cause is the first error in the list. In our case there
    //     is only one root cause,
    //     so that's safe, but you might be careful about that assumption in other contexts.
    //   - `.exception()` gets the actually `Exception` value that was the underlying cause
    Exception exceptionCause = exception.getErrors().get(UserController.AGE_KEY).get(0).exception();
    // The cause should have been a `NumberFormatException` (what is thrown when we try to parse "bad" as an integer).
    assertEquals(NumberFormatException.class, exceptionCause.getClass());
    // The message for that `NumberFOrmatException` should include the text it tried to parse as an integer,
    // i.e., `"bad integer string"`.
    assertTrue(exceptionCause.getMessage().contains(illegalIntegerString));
  }

  /**
   * Test that if the user sends a request with an illegal value in
   * the age field (i.e., too big of a number)
   * we get a reasonable error code back.
   */
  @Test
  void respondsAppropriatelyToTooLargeNumberAge() {
    Map<String, List<String>> queryParams = new HashMap<>();
    String overlyLargeAgeString = "151";
    queryParams.put(UserController.AGE_KEY, Arrays.asList(new String[] {overlyLargeAgeString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    // When the code being tested calls `ctx.queryParam(AGE_KEY)` return the
    // `overlyLargeAgeString`.
    when(ctx.queryParam(UserController.AGE_KEY)).thenReturn(overlyLargeAgeString);

    // Create a validator that confirms that when we ask for the value associated with
    // `AGE_KEY` _as an integer_, we get back the integer value 37.
    Validation validation = new Validation();
    // The `AGE_KEY` should be name of the key whose value is being validated.
    // You can actually put whatever you want here, because it's only used in the generation
    // of testing error reports, but using the actually key value will make those reports more informative.
    Validator<Integer> validator = validation.validator(UserController.AGE_KEY, Integer.class, overlyLargeAgeString);
    when(ctx.queryParamAsClass(UserController.AGE_KEY, Integer.class)).thenReturn(validator);

    // This should now throw a `ValidationException` because
    // our request has an age that is larger than 150, which isn't allowed.
    ValidationException exception = assertThrows(ValidationException.class, () -> {
      userController.getUsers(ctx);
    });
    // This `ValidationException` was caused by a custom check, so we just get the message from the first
    // error and confirm that it contains the problematic string, since that would be useful information
    // for someone trying to debug a case where this validation fails.
    String exceptionMessage = exception.getErrors().get(UserController.AGE_KEY).get(0).getMessage();
    // The message should be the message from our code under test, which should include the text we
    // tried to parse as an age, namely "151".
    assertTrue(exceptionMessage.contains(overlyLargeAgeString));
  }

  /**
   * Test that if the user sends a request with an illegal value in
   * the age field (i.e., too small of a number)
   * we get a reasonable error code back.
   */
  @Test
  void respondsAppropriatelyToTooSmallNumberAge() {
    Map<String, List<String>> queryParams = new HashMap<>();
    String negativeAgeString = "-1";
    queryParams.put(UserController.AGE_KEY, Arrays.asList(new String[] {negativeAgeString}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    // When the code being tested calls `ctx.queryParam(AGE_KEY)` return the
    // `negativeAgeString`.
    when(ctx.queryParam(UserController.AGE_KEY)).thenReturn(negativeAgeString);

    // Create a validator that confirms that when we ask for the value associated with
    // `AGE_KEY` _as an integer_, we get back the string value `negativeAgeString`.
    Validation validation = new Validation();
    // The `AGE_KEY` should be name of the key whose value is being validated.
    // You can actually put whatever you want here, because it's only used in the generation
    // of testing error reports, but using the actually key value will make those reports more informative.
    Validator<Integer> validator = validation.validator(UserController.AGE_KEY, Integer.class, negativeAgeString);
    when(ctx.queryParamAsClass(UserController.AGE_KEY, Integer.class)).thenReturn(validator);

    // This should now throw a `ValidationException` because
    // our request has an age that is larger than 150, which isn't allowed.
    ValidationException exception = assertThrows(ValidationException.class, () -> {
      userController.getUsers(ctx);
    });
    // This `ValidationException` was caused by a custom check, so we just get the message from the first
    // error and confirm that it contains the problematic string, since that would be useful information
    // for someone trying to debug a case where this validation fails.
    String exceptionMessage = exception.getErrors().get(UserController.AGE_KEY).get(0).getMessage();
    // The message should be the message from our code under test, which should include the text we
    // tried to parse as an age, namely "-1".
    assertTrue(exceptionMessage.contains(negativeAgeString));
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
