package org.heat.world.controllers.utils;

import org.heat.User;
import org.rocket.network.PropValidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PropValidation(value = User.class, present = true)
public @interface Authenticated { }
