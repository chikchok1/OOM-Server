/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server.commands;

/**
 *
 * @author YangJinWon
 */
import java.io.*;

public interface Command {

    String execute(String[] params, BufferedReader in, PrintWriter out) throws IOException;
}
