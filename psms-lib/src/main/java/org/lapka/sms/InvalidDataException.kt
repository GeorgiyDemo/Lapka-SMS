package org.lapka.sms

class InvalidDataException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
}
