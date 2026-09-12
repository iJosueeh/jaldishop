package com.jaldishop.backend.identity.application;

public record LoginCommand(
   String email,
   String password
) {}
