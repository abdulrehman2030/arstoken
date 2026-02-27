package com.ar.arstoken.util

import java.util.UUID

fun newCloudId(): String = UUID.randomUUID().toString()

fun nowMs(): Long = System.currentTimeMillis()
