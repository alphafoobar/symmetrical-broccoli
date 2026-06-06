package com.demo.skills.exception;

/** Thrown when an account nickname matches a word on the profanity blocklist. */
public class NicknameNotAllowedException extends RuntimeException {

  /** Creates a new exception. The provided nickname is intentionally excluded from the message. */
  public NicknameNotAllowedException() {
    super("The requested nickname is not allowed");
  }
}
