package com.example.recursiontest;

import android.util.Log;

import com.example.recursiontest.VersionExpressionValidator.VersionCheckException;

import junit.framework.TestCase;

public class TestVersionExpressionValidator extends TestCase {
    
    private static final String TAG = "TestVersionExpressionValidator";
    
    public void testExpressions() {

        String[] expressions = new String[] {
                "[1000, 1000]",
                "(1000, 1000]",
                "[1000, 1000)",
                "(1000, 1000)",
                "[1000, 1203]",
                "[1000, 1203)",
                "(1000, 1203]",
                "(1000, 1203)",
                "(1203, 1000)",
                "(1000a, 1203)",
                "[1000,]",
                "[1000,)",
                "(, 1000)",
                "[, 1000]",
                "(,)"
        };
        
        int total = expressions.length;
        int success = 0;
        for (int i = 0; i < total; i++) {
            String exp = expressions[i];
            try {
                VersionExpressionValidator validator = new VersionExpressionValidator(exp);
                success++;
                Log.d(TAG, exp + "=>" + validator);
            } catch (VersionCheckException e) {
                Log.d(TAG, "illegal version expression " + exp, e);  
            }
        }
        
        Log.d(TAG, "Succeeded " + success + "/" + total);
    
    }

}
