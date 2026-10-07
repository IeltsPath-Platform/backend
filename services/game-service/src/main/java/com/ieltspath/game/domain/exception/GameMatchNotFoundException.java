package com.ieltspath.game.domain.exception;
public class GameMatchNotFoundException extends RuntimeException { public GameMatchNotFoundException(Object id){super("Game match not found: "+id);} }
