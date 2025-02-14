package com.wedasoft.SpaceEngineersModdingHelper.services;

import com.wedasoft.SpaceEngineersModdingHelper.data.configurations.ConfigurationsEntity;
import com.wedasoft.SpaceEngineersModdingHelper.enums.Gender;
import com.wedasoft.SpaceEngineersModdingHelper.exceptions.NotValidException;
import com.wedasoft.SpaceEngineersModdingHelper.repositories.ConfigurationsRepository;
import com.wedasoft.SpaceEngineersModdingHelper.repositories.FileSystemRepository;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CreateNewCharacterModService {

    private static final String CHAR_MALE_TEMPLATE = "CharacterMaleTemplate";
    private static final String CHAR_FEMALE_TEMPLATE = "CharacterFemaleTemplate";
    private static final String CHARACTER_DEFAULT_TEMPLATE = "CharacterDefaultTemplate";

    private final ConfigurationsRepository configurationsRepository;
    private final FileSystemRepository fileSystemRepository;

    public void createNewCharacterMod(
            TextField modNameTextField,
            TextField wishedSubtypeTextField,
            Gender gender,
            boolean devDataDirShallBeCreated,
            boolean createAdditionalFilesForAnAnimalBot) throws NotValidException, IOException, URISyntaxException {

        if (modNameTextField.getText().isBlank()) {
            throw new NotValidException("You must enter a name for your mod.");
        }
        if (wishedSubtypeTextField.getText().isBlank() || !wishedSubtypeTextField.getText().matches("[a-zA-Z_]+")) {
            throw new NotValidException("Your entered subtype for your character is invalid.\nOnly alphabetic characters and the underscore are allowed.");
        }
        if (gender == null) {
            throw new NotValidException("Your entered gender is invalid.");
        }

        final ConfigurationsEntity configurations = configurationsRepository.loadConfigurations();
        if (configurations == null) {
            throw new NotValidException("You must set the configurations first.");
        }
        final Path modsWorkspacePath = Paths.get(configurations.getPathToModsWorkspace());
        if (!Files.exists(modsWorkspacePath)) {
            throw new NotValidException(String.format("""
                    Your configured path to your modding workspace doesn't exist.
                    Your configured path: '%s'""", modsWorkspacePath));
        }

        if (Files.exists(modsWorkspacePath.resolve(modNameTextField.getText()))) {
            throw new NotValidException("A mod with this name does already exist in your modding workspace.");
        }

        final Path modDir = fileSystemRepository.createDirectoryIn(modNameTextField.getText(), modsWorkspacePath);
        final String internalName = wishedSubtypeTextField.getText();

        createThumbnail(modNameTextField, modDir);
        if (devDataDirShallBeCreated) {
            createDevDataDir(modDir, gender);
        }
        createInternalDataSubDir(modDir, internalName, gender, createAdditionalFilesForAnAnimalBot);
        createInternalModelsSubDir(modDir, internalName);
        createInternalTexturesSubDir(modDir, internalName);
    }

    private void createThumbnail(TextField modNameTextField, Path modDir) throws IOException {
        fileSystemRepository.createJpgWithTextContentInto(modNameTextField, modDir);
    }

    private void createDevDataDir(Path modDir, Gender gender) throws IOException, URISyntaxException {
        Path devDataDir = fileSystemRepository.createDirectoryIn("_devData", modDir);
        if (gender == Gender.FEMALE) {
            fileSystemRepository.copyResourceFileInto(
                    getClass().getResource("/seFiles/characterCreation/female/SE_astronaut_female.FBX"),
                    devDataDir);
        } else {
            fileSystemRepository.copyResourceFileInto(
                    getClass().getResource("/seFiles/characterCreation/male/SE_astronaut_male.FBX"),
                    devDataDir);
        }
    }

    private void createInternalDataSubDir(
            Path modDir, String internalName, Gender gender, boolean createAdditionalFilesForAnAnimalBot)
            throws IOException, URISyntaxException {

        final Path dataDir = fileSystemRepository.createDirectoryIn("Data", modDir);
        final Path internalNameSubdir = fileSystemRepository.createDirectoryIn(internalName, dataDir);

        createCharactersSbcAndEntityContainersSbc(gender, internalName, internalNameSubdir);
        if (createAdditionalFilesForAnAnimalBot) {
            createAdditionalFilesForAnAnimalBot(internalName, internalNameSubdir);
        }
    }

    private void createCharactersSbcAndEntityContainersSbc(
            Gender gender, String internalNameForReplacements, Path targetDir)
            throws IOException, URISyntaxException {

        final Map<String, String> replacements = Map.ofEntries(
                Map.entry(CHAR_MALE_TEMPLATE, internalNameForReplacements),
                Map.entry(CHAR_FEMALE_TEMPLATE, internalNameForReplacements));
        if (gender == Gender.MALE) {
            fileSystemRepository.createModifiedSbcFileInto(
                    Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/male/CharacterMaleTemplate_EntityContainers.sbc")).toURI()),
                    targetDir,
                    replacements);
            fileSystemRepository.createModifiedSbcFileInto(
                    Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/male/CharacterMaleTemplate_Characters.sbc")).toURI()),
                    targetDir,
                    replacements);
        } else {
            fileSystemRepository.createModifiedSbcFileInto(
                    Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/female/CharacterFemaleTemplate_EntityContainers.sbc")).toURI()),
                    targetDir,
                    replacements);
            fileSystemRepository.createModifiedSbcFileInto(
                    Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/female/CharacterFemaleTemplate_Characters.sbc")).toURI()),
                    targetDir,
                    replacements);
        }
    }

    private void createAdditionalFilesForAnAnimalBot(
            final String internalNameForReplacements, Path targetDir)
            throws URISyntaxException, IOException {

        final Map<String, String> replacements = Map.ofEntries(
                Map.entry(CHARACTER_DEFAULT_TEMPLATE, internalNameForReplacements));

        // create Stats.sbc
        fileSystemRepository.createModifiedSbcFileInto(
                Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/extraFilesForBots/CharacterDefaultTemplate_Stats.sbc")).toURI()),
                targetDir,
                replacements);

        // create EntityComponents.sbc
        fileSystemRepository.createModifiedSbcFileInto(
                Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/extraFilesForBots/CharacterDefaultTemplate_EntityComponents.sbc")).toURI()),
                targetDir,
                replacements);

        // create Bots.sbc
        fileSystemRepository.createModifiedSbcFileInto(
                Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/extraFilesForBots/CharacterDefaultTemplate_Bots.sbc")).toURI()),
                targetDir,
                replacements);

        // create AIBehavior.sbc
        fileSystemRepository.createModifiedSbcFileInto(
                Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/extraFilesForBots/CharacterDefaultTemplate_AIBehavior.sbc")).toURI()),
                targetDir,
                replacements);

        // create ContainerTypes.sbc
        fileSystemRepository.createModifiedSbcFileInto(
                Path.of(Objects.requireNonNull(getClass().getResource("/seFiles/characterCreation/extraFilesForBots/CharacterDefaultTemplate_ContainerTypes.sbc")).toURI()),
                targetDir,
                replacements);
    }

    private void createInternalModelsSubDir(Path modDir, String internalName) throws IOException {
        final Path models = fileSystemRepository.createDirectoryIn("Models", modDir);
        fileSystemRepository.createDirectoryIn(internalName, models);
    }

    private void createInternalTexturesSubDir(Path modDir, String internalName) throws IOException {
        final Path textures = fileSystemRepository.createDirectoryIn("Textures", modDir);
        fileSystemRepository.createDirectoryIn(internalName, textures);
    }

}
