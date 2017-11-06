package com.alexitc.coinalerts.services

import javax.inject.Inject

import com.alexitc.coinalerts.commons.FutureOr.Implicits._
import com.alexitc.coinalerts.commons.{ApplicationErrors, FutureApplicationResult}
import com.alexitc.coinalerts.data.async.UserAsyncDAL
import com.alexitc.coinalerts.errors.IncorrectPasswordError
import com.alexitc.coinalerts.models._
import com.alexitc.coinalerts.services.validators.UserValidator
import org.mindrot.jbcrypt.BCrypt
import org.scalactic._

import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject() (
    emailService: EmailService,
    userAsyncDAL: UserAsyncDAL,
    userValidator: UserValidator,
    jwtService: JWTService)(
    implicit ec: ExecutionContext) {

  def create(createUserModel: CreateUserModel): Future[User Or ApplicationErrors] = {
    val result = for {
      validatedModel <- userValidator
          .validateCreateUserModel(createUserModel)
          .toFutureOr

      user <- userAsyncDAL
          .create(validatedModel.email, UserHiddenPassword.fromPassword(validatedModel.password))
          .toFutureOr

      token <- userAsyncDAL.createVerificationToken(user.id).toFutureOr

      _ <- emailService.sendVerificationToken(user.email, token).toFutureOr
    } yield user

    result.toFuture
  }

  def verifyEmail(token: UserVerificationToken): FutureApplicationResult[User] = {
    userAsyncDAL.verifyEmail(token)
  }

  def loginByEmail(email: UserEmail, password: UserPassword): FutureApplicationResult[AuthorizationToken] = {
    val result = for {
      _ <- enforcePasswordMatches(email, password).toFutureOr
      user <- userAsyncDAL.getVerifiedUserByEmail(email).toFutureOr
    } yield jwtService.createToken(user.id)

    result.toFuture
  }

  def enforcePasswordMatches(email: UserEmail, password: UserPassword): FutureApplicationResult[Unit] = {
    val result = for {
      existingPassword <- userAsyncDAL.getVerifiedUserPassword(email).toFutureOr
      _ <- Good(BCrypt.checkpw(password.string, existingPassword.string)).filter { matches =>
        if (matches) Pass
        else Fail(One(IncorrectPasswordError))
      }.toFutureOr
    } yield ()

    result.toFuture
  }

  def userById(userId: UserId): FutureApplicationResult[User] = {
    userAsyncDAL.getVerifiedUserById(userId)
  }
}
