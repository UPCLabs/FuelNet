package co.edu.unipiloto.fuelcontrol.api.requests

data class UpdateEmployee(
    var name: String,
    var permissions: List<String>
)
