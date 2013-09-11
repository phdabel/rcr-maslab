package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class MASLABPreProcessamento {
	
	StandardWorldModel model;
	
	MASLABSectoring sectoring;
	File arquivo = new File("MASLABPreProcessamento.txt");
	
	public MASLABPreProcessamento(StandardWorldModel world){
		model = world;
		sectoring = new MASLABSectoring(model);
	}
	
	public MASLABPreProcessamento(MASLABSectoring sec){
		sectoring = sec;
	}
	
	/**
	 * Gera os arquivos de pre processamento
	 */
	public void GerarArquivos(){
		//Grafos dos setores
		GravarMapSetor(sectoring.getMapSetor(1), 1);
		GravarMapSetor(sectoring.getMapSetor(2), 2);
		GravarMapSetor(sectoring.getMapSetor(3), 3);
		GravarMapSetor(sectoring.getMapSetor(4), 4);
		
		//Vias principais
		GravarMapSetor(sectoring.getMapSetor(5), 5);

		//Vias secundárias
		GravarMapSetor(sectoring.getMapSetorSecundarias());
		
		
		//Alocacao dos agentes
		
	}
	
	private void GravarMapSetor(Map<EntityID, List<EntityID>> MapSetor){
		/*
		 * MAPSETOR [Numero do Setor]
		 * CHAVE VIZINHOS
		 * CHAVE VIZINHOS
		 * ...
		 * FIMMAPSETOR
		 */
		try{
			FileWriter fw = new FileWriter(arquivo, true);
			fw.write("MAPSETOR 6\n");
			String values;
			//Para cada chave gera uma linha
			for(EntityID key: MapSetor.keySet()){
				values = "";
				//Pega todos os vizinhos da chave antes de gravar
				for(EntityID v : MapSetor.get(key)){
					if (values.equals("")){
						values = String.valueOf(v.getValue());
					}else{
						values = values + ";" + v.getValue();
					}
				}
				//Grava a linha inteira com a chave e os vizinhos
				fw.write(String.valueOf(key.getValue()) + "-" + values + "\n");
			}
			fw.write("FIMMAPSETOR\n");
	    	fw.flush();
	    	fw.close();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	private void GravarMapSetor(Map<EntityID, Set<EntityID>> MapSetor, int Setor){
		/*
		 * MAPSETOR [Numero do Setor]
		 * CHAVE VIZINHOS
		 * CHAVE VIZINHOS
		 * ...
		 * FIMMAPSETOR
		 */
		try{
			FileWriter fw = new FileWriter(arquivo, true);
			fw.write("MAPSETOR " + String.valueOf(Setor) + "\n");
			String values;
			//Para cada chave gera uma linha
			for(EntityID key: MapSetor.keySet()){
				values = "";
				//Pega todos os vizinhos da chave antes de gravar
				for(EntityID v : MapSetor.get(key)){
					if (values.equals("")){
						values = String.valueOf(v.getValue());
					}else{
						values = values + ";" + v.getValue();
					}
				}
				//Grava a linha inteira com a chave e os vizinhos
				fw.write(String.valueOf(key.getValue()) + "-" + values + "\n");
			}
			fw.write("FIMMAPSETOR\n");
	    	fw.flush();
	    	fw.close();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * Carrega os arquivos gravados
	 */
	public void CarregarArquivo(){
		try {
			FileReader fr = new FileReader(arquivo);
			BufferedReader br = new BufferedReader(fr);
			
			Map<EntityID, Set<EntityID>> MapAux = new HashMap<EntityID, Set<EntityID>>();
			Map<EntityID, List<EntityID>> MapAuxSecondario = new HashMap<EntityID, List<EntityID>>();
			
			int Setor = 0;
			String s;
			int key;
			String[] aux;
			String[] vizinhos;
			List<EntityID> values = new ArrayList<EntityID>();
			
			while((s = br.readLine()) != null) {
				//Novo Setor
				if(s.contains("MAPSETOR ")){
					Setor = Integer.parseInt(s.substring(s.length() - 1, s.length()));
					MapAux = new HashMap<EntityID, Set<EntityID>>();
				//Final do Setor
				}else if(s.contains("FIMMAPSETOR")){
					if(Setor == 1){
						sectoring.MapSetor1 = MapAux;
					}else if(Setor == 2){
						sectoring.MapSetor2 = MapAux;
					}else if(Setor == 3){
						sectoring.MapSetor3 = MapAux;
					}else if(Setor == 4){
						sectoring.MapSetor4 = MapAux;
					}else if(Setor == 5){
						sectoring.MapPrincipal = MapAux;
					}else if(Setor == 6){
						sectoring.MapSecundarias = MapAuxSecondario;
					}
				//Continuacao do Setor
				}else{
					//Separa entre a chave os vizinhos
					aux = s.split("-");
					//A primeira posicao sempre sera a chave
					key = Integer.parseInt(aux[0]);
					//A segunda posicao sera um array separado por ;
					vizinhos = aux[1].split(";");
					values = new ArrayList<EntityID>();
					for(String v : vizinhos){
						values.add(new EntityID(Integer.parseInt(v)));
					}

					if (Setor == 6){
						MapAuxSecondario.put(new EntityID(key), values);
					}else{
						MapAux.put(new EntityID(key), new HashSet<EntityID>(values));
					}
				}
			}
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	public MASLABSectoring getMASLABSectoring(){
		return sectoring;
	}
}
