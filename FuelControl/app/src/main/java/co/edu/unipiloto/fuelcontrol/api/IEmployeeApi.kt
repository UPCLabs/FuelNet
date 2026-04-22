package co.edu.unipiloto.fuelcontrol.api

import co.edu.unipiloto.fuelcontrol.UsuarioRolDto
import co.edu.unipiloto.fuelcontrol.api.requests.UpdateEmployee
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface IEmployeeApi {

    @GET("/api/employees")
    suspend fun getEmployees(): List<UsuarioRolDto>

    @PATCH("/api/employees/{id}")
    suspend fun updatePermissions(
        @Path("id") id: Long,
        @Body request: UpdateEmployee
    )

    @DELETE("/api/employees/{id}")
    suspend fun deleteEmployee(@Path("id") id: Long)
}