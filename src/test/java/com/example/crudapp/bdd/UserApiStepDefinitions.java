package com.example.crudapp.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import com.example.crudapp.model.User; // Assuming User model is accessible

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.withArgs;

public class UserApiStepDefinitions {

    private Response response;
    private User requestUser;
    private Long createdUserId;
    private String baseUri; // To store the base URI from Background

    // Helper map to store users created during tests, keyed by a reference name or email
    private Map<String, User> createdUsersMap = new HashMap<>();


    @Given("The API base URL is {string}")
    public void the_api_base_url_is(String url) {
        this.baseUri = url;
        RestAssured.baseURI = this.baseUri;
    }

    @Given("I have user details with name {string} and email {string}")
    public void i_have_user_details_with_name_and_email(String name, String email) {
        requestUser = new User(name, email);
    }

    @When("I send a POST request to create the user")
    public void i_send_a_post_request_to_create_the_user() {
        response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestUser)
                .when()
                .post(); // Assumes baseURI is already set and POST is to the root of baseURI

        if (response.getStatusCode() == 201) {
            createdUserId = response.jsonPath().getLong("id");
            User createdUser = response.as(User.class);
            // Store the created user with its ID for later retrieval/verification if needed
            createdUsersMap.put(createdUser.getEmail(), createdUser);
            // Also store by ID if we use ID as a reference more often
             if (createdUserId != null) {
                User userFromResponse = response.as(User.class);
                userFromResponse.setId(createdUserId); // Ensure ID is set from response
                createdUsersMap.put(String.valueOf(createdUserId), userFromResponse);
            }
        }
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(int statusCode) {
        response.then().statusCode(statusCode);
    }

    @And("the response should contain the created user details")
    public void the_response_should_contain_the_created_user_details() {
        response.then().body("name", equalTo(requestUser.getName()));
        response.then().body("email", equalTo(requestUser.getEmail()));
        response.then().body("id", notNullValue());
    }

    @And("the user {string} with email {string} should exist in the system")
    public void the_user_with_email_should_exist_in_the_system(String name, String email) {
        // This step verifies by attempting to fetch the user by the ID obtained during creation
        assertNotNull(createdUserId, "User ID was not captured from creation step");

        Response getResponse = RestAssured.given()
                .when()
                .get("/" + createdUserId); // Assumes baseURI + "/{id}"

        getResponse.then().statusCode(200);
        getResponse.then().body("name", equalTo(name));
        getResponse.then().body("email", equalTo(email));
        getResponse.then().body("id", equalTo(createdUserId.intValue())); // jsonPath returns id as int sometimes
    }

    @Given("a user with name {string} and email {string} is created")
    public void a_user_with_name_and_email_is_created(String name, String email) {
        User userToCreate = new User(name, email);
        Response postResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(userToCreate)
                .when()
                .post();
        postResponse.then().statusCode(201); // Ensure creation
        User createdUser = postResponse.as(User.class);
        // Store this user, perhaps using its email or a given reference name as key
        createdUsersMap.put(email, createdUser); // Store by email
        createdUsersMap.put(String.valueOf(createdUser.getId()), createdUser); // Store by ID
    }

    @When("I send a GET request to retrieve all users")
    public void i_send_a_get_request_to_retrieve_all_users() {
        response = RestAssured.given()
                .when()
                .get(); // GET to baseURI
    }

    @And("the response should contain a list of users")
    public void the_response_should_contain_a_list_of_users() {
        response.then().body("$", instanceOf(List.class)); // Checks if the root is a list
    }

    @And("the list should include user {string}")
    public void the_list_should_include_user(String name) {
        // This checks if any user in the list has the given name.
        // More specific checks might require knowing the email or ID.
        response.then().body("find { it.name == '%s' }", withArgs(name), notNullValue());
    }

    // --- Steps for Get User By ID ---
    @When("I send a GET request to retrieve the user by their ID")
    public void i_send_a_get_request_to_retrieve_the_user_by_their_id() {
        // We need a way to get the ID of the user created in a @Given step.
        // Assuming the 'Alice Wonderland' user was the last one stored by email key from a @Given step.
        User userToRetrieve = createdUsersMap.get("alice@example.com");
        assertNotNull(userToRetrieve, "User 'alice@example.com' was not found in created users map.");
        assertNotNull(userToRetrieve.getId(), "ID for 'alice@example.com' is null.");

        response = RestAssured.given()
                .when()
                .get("/" + userToRetrieve.getId());
    }

    @And("the response should contain the details of {string}")
    public void the_response_should_contain_the_details_of(String name) {
        response.then().body("name", equalTo(name));
        // Add email check if possible/needed, requires knowing the expected email for "name"
        User expectedUser = null;
        for (User user : createdUsersMap.values()) {
            if (user.getName().equals(name)) {
                expectedUser = user;
                break;
            }
        }
        assertNotNull(expectedUser, "Could not find expected user " + name + " in map for detail validation");
        response.then().body("email", equalTo(expectedUser.getEmail()));
        response.then().body("id", notNullValue());
    }

    @When("I send a GET request to retrieve user with ID {long}")
    public void i_send_a_get_request_to_retrieve_user_with_id(Long id) {
        response = RestAssured.given()
                .when()
                .get("/" + id);
    }

    // --- Steps for Update User ---
    @When("I send a PUT request to update the user by their ID with new name {string} and email {string}")
    public void i_send_a_put_request_to_update_the_user_by_their_id_with_new_name_and_email(String newName, String newEmail) {
        // Assuming the user "Old Name" was stored by its email "old.email@example.com"
        User userToUpdate = createdUsersMap.get("old.email@example.com");
        assertNotNull(userToUpdate, "User 'old.email@example.com' not found for update.");
        assertNotNull(userToUpdate.getId(), "ID for 'old.email@example.com' is null.");

        User updatedUserDetails = new User(newName, newEmail);

        response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updatedUserDetails)
                .when()
                .put("/" + userToUpdate.getId());

        if (response.getStatusCode() == 200) {
            this.createdUserId = userToUpdate.getId(); // Keep track of the ID for verification
            // Update map if necessary, or rely on subsequent verification step
            User updatedUserFromResponse = response.as(User.class);
            createdUsersMap.put(updatedUserFromResponse.getEmail(), updatedUserFromResponse);
            createdUsersMap.put(String.valueOf(updatedUserFromResponse.getId()), updatedUserFromResponse);
        }
    }

    @And("the response should contain the updated user details with name {string}")
    public void the_response_should_contain_the_updated_user_details_with_name(String newName) {
        response.then().body("name", equalTo(newName));
        response.then().body("id", notNullValue());
        // Could also check email if it's part of the scenario's expectation
    }

    @And("the user {string} with email {string} should exist in the system with that ID")
    public void the_user_with_email_should_exist_in_the_system_with_that_id(String name, String email) {
        assertNotNull(createdUserId, "User ID was not captured from the update/creation step for verification.");

        Response getResponse = RestAssured.given()
                .when()
                .get("/" + createdUserId);

        getResponse.then().statusCode(200);
        getResponse.then().body("name", equalTo(name));
        getResponse.then().body("email", equalTo(email));
        getResponse.then().body("id", equalTo(createdUserId.intValue()));
    }

    @When("I send a PUT request to update user with ID {long} with new name {string} and email {string}")
    public void i_send_a_put_request_to_update_user_with_id_with_new_name_and_email(Long id, String newName, String newEmail) {
        User userDetails = new User(newName, newEmail);
        response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(userDetails)
                .when()
                .put("/" + id);
    }

    // --- Steps for Delete User ---
    @When("I send a DELETE request to remove the user by their ID")
    public void i_send_a_delete_request_to_remove_the_user_by_their_id() {
        // Assuming user "User ToDelete" was stored by its email
        User userToDelete = createdUsersMap.get("todelete@example.com");
        assertNotNull(userToDelete, "User 'todelete@example.com' not found for deletion.");
        assertNotNull(userToDelete.getId(), "ID for 'todelete@example.com' is null.");

        this.createdUserId = userToDelete.getId(); // Store ID for verification step

        response = RestAssured.given()
                .when()
                .delete("/" + userToDelete.getId());
    }

    @And("the user with that ID should no longer exist in the system")
    public void the_user_with_that_id_should_no_longer_exist_in_the_system() {
        assertNotNull(createdUserId, "User ID was not captured for delete verification.");
        Response getResponse = RestAssured.given()
                .when()
                .get("/" + createdUserId);

        getResponse.then().statusCode(404); // Expect NOT_FOUND
    }

    @When("I send a DELETE request to remove user with ID {long}")
    public void i_send_a_delete_request_to_remove_user_with_id(Long id) {
        response = RestAssured.given()
                .when()
                .delete("/" + id);
    }
}
