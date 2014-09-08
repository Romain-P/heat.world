package org.heat.world.controllers.utils;

import org.heat.world.users.WorldUser;
import org.rocket.network.PropValidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PropValidation(value = WorldUser.class, present = true)
public @interface Authenticated { }
