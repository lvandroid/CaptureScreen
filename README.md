# Android截屏

## 使用说明

```java
//在Activity onCreate()方法里初始化
CaptureManager manager =CaptureManager.getInstance(this);
manager.init(this);

//重写onDestroy()
   @Override
     protected void onDestroy() {
         super.onDestroy();
         manager.destroy();
     }

//重写onActivityResult(）

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        manager.onActivityResult(requestCode, resultCode, data);
    }

```
