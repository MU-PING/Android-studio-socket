# Android-studio-socket
## 程式簡介
### 簡述
* `Android studio`開發
* 使用`Socket`實作`Android chat APP`
### 範例圖
![20211204_060223](https://user-images.githubusercontent.com/93152909/155802829-612c664a-0b7c-486a-8b6e-849a33df4584.gif)

## Socket簡介
* socket是一種作業系統提供的行程間通訊機制
* 從網路的角度來看，socket就是通訊連結的端點；從程式設計者的角度來看，socket提供了一個良好的介面，使程式設計者不需知道下層網路協定運作的細節便可以撰寫網路通訊程式
* 使用者或應用程式只要連結到 socket 便可以和網路上任何一個通訊端點連線
### Socket通訊流程
<img src="https://user-images.githubusercontent.com/93152909/155818903-809ef0e6-cd83-41f7-a076-e860d94545f7.png" width="600">

### Android studio使用Socket
#### 權限設定
在 `AndroidManifest.xml`檔案中，必須設定網路存取權限

![圖片1](https://user-images.githubusercontent.com/93152909/155818842-33a44de0-b062-4004-a862-27d5105ea456.png)

#### 執行緒
* Android 執行緒分兩種 :
  * 主執行緒：也稱UI執行緒，負責更新UI介面，**禁止存取網路**
  * 一般執行緒：可執行所有雜事，可存取網路，禁止更新UI介面
  
* Android中系統不允許在非Main Thread (UI Thread)中更新UI 

* 若一般的執行緒想要修改UI內容的話可以使用 
  * `runOnUiThread()`
  * `Handler`
#### Socket函式 - Server
* `ServerSocket(int port)`：建構監聽指定Port，Port範圍介於1024 ~ 65535，其中0 ~ 1023為系統保留不可使用
* `accept()`：客戶端的Socket未發出連線請求，伺服器就會一直等待，直到客戶端發送連線請求，請求成功後返回一個與客戶端對應的服務端Socket，如果設置Timeout時間，當伺服器等待的時間超過了超時時間,就會拋出異常SocketTimeoutException
* `isClosed()`：查詢連接是否已關閉
* `close()`：關閉此ServerSocket

#### Socket函式 - Client
* `Socket(String host, int port)`：建構一個串流，連接到指定的主機位置及指定Port。
* `isConnected()`：是否連線。
* `getInputStream()`：取得Socket輸入流，用於接收送過來的資料。
* `getOutputStream()`：取得Socket輸出流，發送資料。
* `getInetAddress().getHostAddress()`：取得客戶端的IP位置。

####  訊息的傳遞
* 使用JSON檔案傳遞，依照 JavaScript 物件語法的資料格式
* JSON 可能是物件或字串
  * 當你想從 JSON中讀取資料時，JSON可作為物件
  * 當要跨網路傳送 JSON 時，就會是字串。




