package it.ecubit.elabora.lul.tools.model;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
public class Company implements Serializable {

	private static final long serialVersionUID = 7542705588532069944L;
	
	private String name;
	private String address;

}
