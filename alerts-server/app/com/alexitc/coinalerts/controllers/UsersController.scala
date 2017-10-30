package com.alexitc.coinalerts.controllers

import javax.inject.Inject

import com.alexitc.coinalerts.commons.FutureOr.Implicits._
import com.alexitc.coinalerts.models.{CreateUserModel, UserCreatedModel}
import com.alexitc.coinalerts.services.UserService

class UsersController @Inject() (userService: UserService) extends JsonController {

  def create() = async[CreateUserModel, UserCreatedModel] { createUserModel =>
    val result = for {
      createdUser <- userService
          .create(createUserModel)
          .toFutureOr

    } yield UserCreatedModel(createdUser.id, createdUser.email)

    result.toFuture
  }
}