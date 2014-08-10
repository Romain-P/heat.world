package org.heat.world.controllers.utils;

import org.heat.world.players.Player;
import org.rocket.network.PropValidation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Authenticated
@PropValidation(value = Player.class, present = true)
public @interface RolePlaying { }
