package axoloti.utils;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

import java.util.Arrays;
import java.util.Base64;

public class PasswordConverter implements Converter<char[]> {

    @Override
    public char[] read(InputNode node) throws Exception {
        String encodedPassword = node.getValue();
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return null;
        }

        byte[] decodedBytes = Base64.getDecoder().decode(encodedPassword);
        char[] password = new char[decodedBytes.length];
        for (int i = 0; i < decodedBytes.length; i++) {
            password[i] = (char) decodedBytes[i];
        }
        Arrays.fill(decodedBytes, (byte)0);

        return password;
    }

    @Override
    public void write(OutputNode node, char[] password) throws Exception {
        if (password == null || password.length == 0) {
            return;
        }

        byte[] bytes = new byte[password.length];
        for (int i = 0; i < password.length; i++) {
            bytes[i] = (byte) password[i];
        }

        String encodedPassword = Base64.getEncoder().encodeToString(bytes);
        node.setValue(encodedPassword);
        Arrays.fill(bytes, (byte)0);
    }
}