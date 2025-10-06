package axoloti.displays;

import java.nio.ByteBuffer;

import components.XYGraphComponent;

public class DisplayInstanceFrac8S128XY extends DisplayInstance<DisplayFrac8S128XY> {

    final int n = 128; 

    final int totalBytes = n * 2; 

    public DisplayInstanceFrac8S128XY() {
        super();
    }

    private XYGraphComponent xygraph;

    @Override
    public void PostConstructor() {
        super.PostConstructor();
        xygraph = new XYGraphComponent(n, n, n, -64, 64, -64, 64);
        add(xygraph);
    }

    @Override
    public String GenerateCodeInit(String vprefix) {
        String s = "{\n"
                 + "    int _i;\n"
                 + "    for (_i = 0; _i < " + totalBytes + "; _i++)\n"
                 + "    " + GetCName() + "[_i] = 0;\n"
                 + "}\n";
        return s;
    }

    @Override
    public String valueName(String vprefix) {
        return "(int8_t*) (&displayVector[" + offset + "])"; 
    }

    byte dst[] = new byte[totalBytes]; 
    int xData[] = new int[n];
    int yData[] = new int[n];

    @Override
    public void ProcessByteBuffer(ByteBuffer bb) {
        bb.get(dst);

        for (int i = 0; i < n; i++) {
            xData[i] = dst[i];
            yData[i] = dst[i + n]; 
        }
        xygraph.setValue(xData, yData);
    }

    @Override
    public void updateV() {
    }

    @Override
    public int getLength() {
        return totalBytes / 4; 
    }
}