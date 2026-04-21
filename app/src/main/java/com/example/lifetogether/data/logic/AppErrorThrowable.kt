package com.example.lifetogether.data.logic

import com.example.lifetogether.domain.result.AppError

class AppErrorThrowable(val appError: AppError) : Throwable()