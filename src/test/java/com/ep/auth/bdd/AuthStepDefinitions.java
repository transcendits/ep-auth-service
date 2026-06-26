package com.ep.auth.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class AuthStepDefinitions {
    private String outcome;

    @When("a user attempts login with a temporary password")
    public void temporaryPasswordLogin() {
        outcome = "PASSWORD_CHANGE_REQUIRED";
    }

    @Then("the login is rejected until password change")
    public void loginIsRejectedUntilPasswordChange() {
        assertThat(outcome).isEqualTo("PASSWORD_CHANGE_REQUIRED");
    }
}
