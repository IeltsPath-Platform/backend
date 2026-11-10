package com.ieltspath.user.application.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleIdentityTest {
    @Test
    void onlyTreatsGmailOrMatchingHostedDomainsAsAuthoritative() {
        assertThat(new GoogleIdentity("s", "person@gmail.com", "Person", null).isEmailAuthoritative()).isTrue();
        assertThat(new GoogleIdentity("s", "person@googlemail.com", "Person", null).isEmailAuthoritative()).isTrue();
        assertThat(new GoogleIdentity("s", "person@school.edu", "Person", "SCHOOL.EDU").isEmailAuthoritative()).isTrue();
        assertThat(new GoogleIdentity("s", "person@school.edu", "Person", "other.edu").isEmailAuthoritative()).isFalse();
        assertThat(new GoogleIdentity("s", "person@hotmail.com", "Person", null).isEmailAuthoritative()).isFalse();
    }
}
