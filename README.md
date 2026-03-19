# Seguranca-Projeto1

---

## Username rules:
 - can't contain spaces;
 - can't contain special characters;
 - can't be empty;

## Format of files:

- ### Houses.txt: 
  - `house_id; {Section_id[Device_list]; ...}`
- ### User.txt: 
  - `user_id:password` for each line 
- ### Perms.txt: 
  - `house_id [user_id:section_id,...; user_id:section_id,...]` for each house 1 line with all the permissions for each section

## Duvidas

- se o mesmo utilizador tentar entrar em dois clientes diferentes, ao mesmo tempo, o mesmo utilizador pode ser
  bloqueado? ou desconectar o primeiro?
- 