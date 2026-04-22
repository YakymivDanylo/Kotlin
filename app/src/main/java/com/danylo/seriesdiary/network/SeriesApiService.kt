package com.danylo.seriesdiary.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SeriesApiService {

    @GET("series")
    suspend fun getAllSeries(): List<SeriesDto>

    @GET("series/{id}")
    suspend fun getSeriesById(@Path("id") id: String): SeriesDto

    @POST("series")
    suspend fun createSeries(@Body body: CreateSeriesRequest): SeriesDto

    @DELETE("series/{id}")
    suspend fun deleteSeries(@Path("id") id: String): SeriesDto
}
