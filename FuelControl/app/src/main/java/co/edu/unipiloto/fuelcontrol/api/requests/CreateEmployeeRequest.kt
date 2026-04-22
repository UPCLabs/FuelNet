package co.edu.unipiloto.fuelcontrol.api.requests

data class CreateEmployeeRequest(
    val name: String,
    val email: String,
    val password: String,
    val permissions: List<String>
)
