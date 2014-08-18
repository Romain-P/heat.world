package org.heat.world.controllers.utils;

import org.heat.world.roleplay.WorldAction;
import org.rocket.network.PropValidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@RolePlaying
@PropValidation(value = WorldAction.class, present = false)
public @interface Idling { }
