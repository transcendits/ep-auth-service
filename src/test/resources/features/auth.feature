Feature: Authentication rules
  Scenario: Temporary password cannot be used for login
    When a user attempts login with a temporary password
    Then the login is rejected until password change
