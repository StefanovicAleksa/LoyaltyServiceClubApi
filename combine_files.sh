#!/bin/bash

# Function to show usage
show_usage() {
    echo "Usage: $0 folder1 [folder2 ...] [output_file] [ignore_file] [-v]"
    echo "  folder1, folder2, ... : Folders to process (required)"
    echo "  output_file          : Output file name (default: CombinedOutput.txt)"
    echo "  ignore_file          : Ignore patterns file (default: .fileignore)"
    echo "  -v                   : Verbose output"
    echo ""
    echo "Examples:"
    echo "  $0 /path/to/folder"
    echo "  $0 /path/to/folder output.txt"
    echo "  $0 /path/to/folder output.txt .myignore"
    echo "  $0 /path/to/folder1 /path/to/folder2 -v"
}

# Initialize variables
declare -a folder_paths=()
output_file="CombinedOutput.txt"
ignore_file=".fileignore"
verbose=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            verbose=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        -*)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
        *)
            # Check if it's a directory
            if [[ -d "$1" ]]; then
                folder_paths+=("$1")
            else
                # Not a directory, could be output file or ignore file
                if [[ ${#folder_paths[@]} -eq 0 ]]; then
                    echo "Error: No folders specified"
                    show_usage
                    exit 1
                fi
                
                # Determine if this is output file or ignore file based on position
                if [[ "$output_file" == "CombinedOutput.txt" ]]; then
                    output_file="$1"
                elif [[ "$ignore_file" == ".fileignore" ]]; then
                    ignore_file="$output_file"
                    output_file="$1"
                fi
            fi
            shift
            ;;
    esac
done

# Check if we have at least one folder
if [[ ${#folder_paths[@]} -eq 0 ]]; then
    echo "Error: At least one folder path is required"
    show_usage
    exit 1
fi

# Function to check if a file matches ignore patterns
test_is_ignored() {
    local file_path="$1"
    local patterns=("${@:2}")
    
    # Normalize path (convert backslashes to forward slashes)
    local normalized_path="${file_path//\\//}"
    
    for pattern in "${patterns[@]}"; do
        # Skip empty lines and comments
        [[ -z "$pattern" || "$pattern" =~ ^[[:space:]]*# ]] && continue
        
        # Trim whitespace and normalize
        pattern=$(echo "$pattern" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | tr '\\' '/')
        [[ -z "$pattern" ]] && continue
        
        # Direct match
        if [[ "$normalized_path" == "$pattern" ]]; then
            echo "true|$pattern"
            return 0
        fi
        
        # File extension match (*.ext)
        if [[ "$pattern" =~ ^\*\. ]]; then
            local extension="${pattern:1}"
            if [[ "$normalized_path" == *"$extension" ]]; then
                echo "true|$pattern"
                return 0
            fi
        fi
        
        # Double-star match
        if [[ "$pattern" == *"**"* ]]; then
            # Handle special case of **/path/
            if [[ "$pattern" =~ ^\*\*/ ]]; then
                local path="${pattern:3}"
                if [[ "$normalized_path" == *"/$path"* ]]; then
                    echo "true|$pattern"
                    return 0
                fi
            else
                # Split on **
                local prefix="${pattern%%\*\**}"
                local suffix="${pattern##*\*\*}"
                
                local match=true
                if [[ -n "$prefix" && "$normalized_path" != "$prefix"* ]]; then
                    match=false
                fi
                
                if [[ -n "$suffix" && "$normalized_path" != *"$suffix" ]]; then
                    match=false
                fi
                
                if [[ "$match" == "true" ]]; then
                    echo "true|$pattern"
                    return 0
                fi
            fi
        fi
        
        # Single-star match (glob pattern)
        if [[ "$pattern" == *"*"* ]]; then
            if [[ "$normalized_path" == $pattern ]]; then
                echo "true|$pattern"
                return 0
            fi
        fi
        
        # Directory match
        if [[ "$pattern" == */ ]]; then
            local dir_pattern="${pattern%/}"
            if [[ "$normalized_path" == "$dir_pattern"* ]]; then
                echo "true|$pattern"
                return 0
            fi
        fi
    done
    
    echo "false|"
    return 1
}

# Load ignore patterns
declare -a ignore_patterns=()
if [[ -f "$ignore_file" ]]; then
    while IFS= read -r line; do
        # Skip empty lines and comments
        line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        if [[ -n "$line" && ! "$line" =~ ^# ]]; then
            ignore_patterns+=("$line")
        fi
    done < "$ignore_file"
    echo "Loaded ${#ignore_patterns[@]} patterns from $ignore_file"
else
    echo "No ignore file found at $ignore_file"
fi

# Create temporary file for output content
temp_file=$(mktemp)
trap "rm -f $temp_file" EXIT

# Process each folder
for folder_path in "${folder_paths[@]}"; do
    if [[ ! -d "$folder_path" ]]; then
        echo "Error: Folder not found: $folder_path" >&2
        continue
    fi
    
    # Get absolute path
    full_folder_path=$(realpath "$folder_path")
    
    # Add folder header
    echo "# ===== Files from: $folder_path =====" >> "$temp_file"
    echo "" >> "$temp_file"
    
    processed_count=0
    ignored_count=0
    
    # Find all files recursively
    while IFS= read -r -d '' file; do
        # Get relative path
        relative_path="${file#$full_folder_path/}"
        
        # Check if file should be ignored
        result=$(test_is_ignored "$relative_path" "${ignore_patterns[@]}")
        should_ignore="${result%%|*}"
        matched_pattern="${result##*|}"
        
        if [[ "$should_ignore" == "true" ]]; then
            ((ignored_count++))
            if [[ "$verbose" == "true" ]]; then
                echo "  Ignored: $relative_path (pattern: $matched_pattern)"
            fi
            continue
        fi
        
        # Add file to output
        echo "# File: $relative_path" >> "$temp_file"
        cat "$file" >> "$temp_file"
        echo "" >> "$temp_file"
        
        ((processed_count++))
        if [[ "$verbose" == "true" ]]; then
            echo "  Added: $relative_path"
        fi
        
    done < <(find "$full_folder_path" -type f -print0)
    
    # Add folder footer
    echo "# ===== End of files from: $folder_path =====" >> "$temp_file"
    echo "" >> "$temp_file"
    
    echo "Processed $processed_count files from $folder_path (ignored $ignored_count files)"
done

# Move temp file to final output
mv "$temp_file" "$output_file"
echo "Combined file created: $output_file"