package org.cryptotrade.helper;

import java.util.HashMap;
import java.util.Map;

import org.cryptotrade.entity.AccountInfo;
import org.cryptotrade.entity.AssetPropsEnum;
import org.cryptotrade.entity.PositionPropsEnum;
import org.json.JSONArray;
import org.json.JSONObject;

public class AccountAccessHelper {
	private AccountInfo account = new AccountInfo();;
	
	public AccountAccessHelper(String apiKey, String apiSecret) {
		account.apiKey = apiKey;
		account.apiSecret = apiSecret;
	}
	
	public void updateAccount() {
		updateAsset();
		updatePosition();
	}
	
	public void updateAsset() {
		// TODO 等待API套件完成
		JSONObject json = null;
		
		JSONArray result = json.getJSONArray("result");
		Map<String,Object> assetData = new HashMap<>();
		for(int i=0; i<result.length(); i++) {
			JSONObject data = result.getJSONObject(i);
			String coin = data.getString(AssetPropsEnum.Coin);
			assetData.put(AssetPropsEnum.Total, data.getDouble(AssetPropsEnum.Total));
			assetData.put(AssetPropsEnum.Free, data.getDouble(AssetPropsEnum.Free));
			assetData.put(AssetPropsEnum.AvailableForWithdrawal, data.getDouble(AssetPropsEnum.AvailableForWithdrawal));
			assetData.put(AssetPropsEnum.AvailableWithoutBorrow, data.getDouble(AssetPropsEnum.AvailableWithoutBorrow));
			assetData.put(AssetPropsEnum.UsdValue, data.getDouble(AssetPropsEnum.UsdValue));
			assetData.put(AssetPropsEnum.SpotBorrow, data.getDouble(AssetPropsEnum.SpotBorrow));
			account.balance.put(coin, assetData);
		}
	}
	
	public void updatePosition() {
		// TODO 等待API套件完成
		JSONObject json = null;
		
		JSONArray result = json.getJSONArray("result");
		Map<String,Object> positionData = new HashMap<>();
		for(int i=0; i<result.length(); i++) {
			JSONObject data = result.getJSONObject(i);
			String futureName = data.getString(PositionPropsEnum.Future);
			positionData.put(PositionPropsEnum.Size, data.getDouble(PositionPropsEnum.Size));
			positionData.put(PositionPropsEnum.Side, data.getString(PositionPropsEnum.Side));
			positionData.put(PositionPropsEnum.NetSize, data.getDouble(PositionPropsEnum.NetSize));
			positionData.put(PositionPropsEnum.LongOrderSize, data.getDouble(PositionPropsEnum.LongOrderSize));
			positionData.put(PositionPropsEnum.ShortOrderSize, data.getDouble(PositionPropsEnum.ShortOrderSize));
			positionData.put(PositionPropsEnum.Cost,data.getDouble(PositionPropsEnum.Cost));
			positionData.put(PositionPropsEnum.EntryPrice,data.getDouble(PositionPropsEnum.EntryPrice));
			positionData.put(PositionPropsEnum.UnrealizedPnl,data.getDouble(PositionPropsEnum.UnrealizedPnl));
			positionData.put(PositionPropsEnum.RealizedPnl,data.getDouble(PositionPropsEnum.RealizedPnl));
			positionData.put(PositionPropsEnum.InitialMarginRequirement,data.getDouble(PositionPropsEnum.InitialMarginRequirement));
			positionData.put(PositionPropsEnum.MaintenanceMarginRequirement,data.getDouble(PositionPropsEnum.MaintenanceMarginRequirement));
			positionData.put(PositionPropsEnum.OpenSize,data.getDouble(PositionPropsEnum.OpenSize));
			positionData.put(PositionPropsEnum.CollateralUsed,data.getDouble(PositionPropsEnum.CollateralUsed));
			positionData.put(PositionPropsEnum.EstimatedLiquidationPrice,data.getDouble(PositionPropsEnum.EstimatedLiquidationPrice));
			account.balance.put(futureName, positionData);
		}
	}
	
	public void updateMarginData() { updateMarginData(false);}
	
	public void updateMarginData(boolean updateBasicDataFlag) {
		if(updateBasicDataFlag) updatePosition();
		
	}
	
	
}
