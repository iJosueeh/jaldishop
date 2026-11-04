package com.jaldishop.backend.identity.application.command;

public record LoginCommand(
   String email,
   String password
) {}
