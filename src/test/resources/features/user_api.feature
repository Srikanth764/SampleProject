Feature: User API Management
  As a client of the User API
  I want to perform CRUD operations on users
  So that I can manage user data

  Background:
    * The API base URL is "http://localhost:8080/api/users"

  Scenario: Successfully create a new user
    Given I have user details with name "John Doe" and email "john.doe@example.com"
    When I send a POST request to create the user
    Then the response status should be 201
    And the response should contain the created user details
    And the user "John Doe" with email "john.doe@example.com" should exist in the system

  Scenario: Retrieve all users
    Given a user with name "Jane Smith" and email "jane.smith@example.com" is created
    And a user with name "Peter Jones" and email "peter.jones@example.com" is created
    When I send a GET request to retrieve all users
    Then the response status should be 200
    And the response should contain a list of users
    And the list should include user "Jane Smith"
    And the list should include user "Peter Jones"

  Scenario: Retrieve a specific user by ID
    Given a user with name "Alice Wonderland" and email "alice@example.com" is created
    When I send a GET request to retrieve the user by their ID
    Then the response status should be 200
    And the response should contain the details of "Alice Wonderland"

  Scenario: Attempt to retrieve a non-existent user by ID
    # Assuming ID 99999 is unlikely to exist before test execution
    When I send a GET request to retrieve user with ID 99999
    Then the response status should be 404

  Scenario: Successfully update an existing user
    Given a user with name "Old Name" and email "old.email@example.com" is created
    When I send a PUT request to update the user by their ID with new name "New Name" and email "new.email@example.com"
    Then the response status should be 200
    And the response should contain the updated user details with name "New Name"
    And the user "New Name" with email "new.email@example.com" should exist in the system with that ID

  Scenario: Attempt to update a non-existent user
    # Assuming ID 88888 is unlikely to exist
    When I send a PUT request to update user with ID 88888 with new name "Any Name" and email "any.email@example.com"
    Then the response status should be 404

  Scenario: Successfully delete an existing user
    Given a user with name "User ToDelete" and email "todelete@example.com" is created
    When I send a DELETE request to remove the user by their ID
    Then the response status should be 204
    And the user with that ID should no longer exist in the system

  Scenario: Attempt to delete a non-existent user
    # Assuming ID 77777 is unlikely to exist
    When I send a DELETE request to remove user with ID 77777
    Then the response status should be 404
