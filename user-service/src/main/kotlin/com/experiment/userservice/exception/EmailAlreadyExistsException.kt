package com.experiment.userservice.exception

class EmailAlreadyExistsException(
    message: String = "User email already exists"
) : RuntimeException(message)