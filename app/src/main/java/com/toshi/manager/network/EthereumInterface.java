/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.manager.network;

import com.toshi.model.network.Balance;
import com.toshi.model.network.token.ERC721Tokens;
import com.toshi.model.network.GcmDeregistration;
import com.toshi.model.network.GcmRegistration;
import com.toshi.model.network.SentTransaction;
import com.toshi.model.network.ServerTime;
import com.toshi.model.network.SignedTransaction;
import com.toshi.model.network.token.ERC20Tokens;
import com.toshi.model.network.TransactionRequest;
import com.toshi.model.network.UnsignedTransaction;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Single;

public interface EthereumInterface {

    @POST("/v1/tx/skel")
    Single<UnsignedTransaction> createTransaction(@Body TransactionRequest request);

    @POST("/v1/tx")
    Single<SentTransaction> sendSignedTransaction(
            @Query("timestamp") long timestamp,
            @Body SignedTransaction transaction);


    @GET("/v1/balance/{id}")
    Single<Balance> getBalance(@Path("id") String walletAddress);

    @GET("/v1/timestamp")
    Single<ServerTime> getTimestamp();

    @POST("/v1/gcm/register")
    Single<Void> registerGcm(
            @Query("timestamp") long timestamp,
            @Body GcmRegistration gcmRegistration);

    @POST("/v1/gcm/deregister")
    Single<Void> unregisterGcm(
            @Query("timestamp") long timestamp,
            @Body GcmDeregistration gcmDeregistration);

    @GET("/v1/tokens/{id}")
    Single<ERC20Tokens> getTokens(@Path("id") String walletAddress);

    @GET("/v1/collectibles/{id}")
    Single<ERC721Tokens> getCollectibles(@Path("id") String walletAddress);
}
