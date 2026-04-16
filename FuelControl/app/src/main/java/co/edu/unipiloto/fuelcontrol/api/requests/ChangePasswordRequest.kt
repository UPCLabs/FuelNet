package co.edu.unipiloto.fuelcontrol.api.requests

data class ChangePasswordRequest (
    val oldPassword: String,
    val newPassword: String
)