# Nukkit Image plugin
You can place your image in your Nukkit server.

![Minecraft 2021-02-13 3_34_55 AM](https://user-images.githubusercontent.com/29895665/107820213-f9f02700-6dac-11eb-8563-c30266a49a70.png)

## Commands
```
- /ip place <image name>
- /ip remove
```

## API
The description will be added soon.
```java
ImagePlugin.breakImage(BlockEntityItemFrame blockEntity);
ImagePlugin.placeImage(RequestInformation info);
ImageUtils.getSplitImageMap(int lx, int ly, BufferedImage image, @Nullable Map<String, String> data);
```